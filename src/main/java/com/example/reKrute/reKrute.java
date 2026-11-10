package com.example.reKrute;

import com.example.Job;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Offres au Maroc : https://www.rekrute.com/offres-emploi-maroc.html.
 * En cas d'erreur PKIX sous Windows, utiliser le magasin de certificats Windows
 * avec les options VM : -Djavax.net.ssl.trustStoreType=Windows-ROOT
 * -Djavax.net.ssl.trustStore=NONE (la verification TLS reste active).
 */
public class reKrute {
    private static final String SITE = "https://www.rekrute.com/offres-emploi-maroc.html";
    private final ArrayList<Job> emploiDispo = new ArrayList<>();
    private final Map<String, String> cookies = new HashMap<>();
    private long lastRequest = 0;

    public reKrute(boolean watch) {
        this(watch, 2, Integer.MAX_VALUE);
    }

    /** maxPages et maxJobs limitent les requetes et doivent etre superieurs a zero. */
    public reKrute(boolean watch, int maxPages, int maxJobs) {
        if (maxPages < 1 || maxJobs < 1) {
            throw new IllegalArgumentException("Les limites doivent etre strictement positives.");
        }
        Set<String> visitedPages = new HashSet<>();
        Set<String> visitedJobs = new HashSet<>();
        String pageUrl = SITE;
        for (int page = 1; page <= maxPages && emploiDispo.size() < maxJobs; page++) {
            if (pageUrl.isEmpty() || !visitedPages.add(pageUrl)) {
                break;
            }
            Document listing;
            try {
                listing = fetch(pageUrl);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("ReKrute : page inaccessible " + pageUrl + " : " + e.getMessage());
                break;
            }

            Elements cards = listing.select("ul.job-list#post-data > li.post-id");
            if (cards.isEmpty()) {
                break;
            }
            for (Element card : cards) {
                if (emploiDispo.size() >= maxJobs) {
                    break;
                }
                Element link = card.selectFirst("h2 a.titreJob[href]");
                // La pagination peut aussi proposer des annonces hors du Maroc.
                if (link == null || !link.text().toLowerCase(Locale.ROOT).endsWith("(maroc)")) {
                    continue;
                }
                String url = siteUrl(link.absUrl("href"));
                if (url.isEmpty() || !visitedJobs.add(url)) {
                    continue;
                }
                try {
                    Job job = parseJob(fetch(url));
                    if (job != null) {
                        emploiDispo.add(job);
                        if (watch) {
                            System.out.printf("ReKrute - page %d - offre %d : %s%n",
                                    page, emploiDispo.size(), job.getTitle());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (HttpStatusException e) {
                    System.err.println("ReKrute : HTTP " + e.getStatusCode() + " pour " + url);
                    if (e.getStatusCode() == 403 || e.getStatusCode() == 429) {
                        return;
                    }
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("ReKrute : offre ignoree " + url + " : " + e.getMessage());
                }
            }
            Element next = listing.selectFirst(".pagination a.next[href]");
            pageUrl = next == null ? "" : siteUrl(next.absUrl("href"));
        }
    }

    private Document fetch(String url) throws IOException, InterruptedException {
        long pause = 1000 - (System.currentTimeMillis() - lastRequest);
        if (pause > 0) {
            Thread.sleep(pause);
        }
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; OffreScraping/1.0)")
                    .timeout(30000)
                    .maxBodySize(5 * 1024 * 1024)
                    .cookies(cookies)
                    .execute();
            cookies.putAll(response.cookies());
            return response.parse();
        } finally {
            lastRequest = System.currentTimeMillis();
        }
    }

    /** Selecteurs verifies sur les fiches ReKrute du 18/10/2026. */
    static Job parseJob(Document document) {
        JSONObject metadata = jobMetadata(document);
        String title = metadata.optString("title", "").trim();
        if (title.isEmpty()) {
            title = document.select("#BlocTitreAnnonce .jo-topTitle").text();
        }
        if (title.isEmpty()) {
            return null;
        }

        JSONObject location = metadata.optJSONObject("jobLocation");
        JSONObject address = location == null ? null : location.optJSONObject("address");
        String city = address == null ? "" : address.optString("addressLocality", "").trim();
        String country = address == null ? "" : address.optString("addressCountry", "").trim();
        if (!country.isEmpty() && !country.equalsIgnoreCase("Maroc")
                && !country.equalsIgnoreCase("MA") && !country.equalsIgnoreCase("Morocco")) {
            return null;
        }
        if (city.isEmpty()) {
            city = document.select(".jo-pill--cityRegion").text()
                    .replaceFirst("(?i)^\\d+\\s+poste\\(s\\)\\s+sur\\s+", "")
                    .replaceFirst("(?i)\\s*-\\s*Maroc$", "").trim();
        }
        String publication = metadata.optString("datePosted", "").trim();
        if (publication.isEmpty()) {
            publication = document.select(".jo-pill--publication").text()
                    .replace("sur ReKrute.com", "").trim();
        }
        JSONObject employer = metadata.optJSONObject("hiringOrganization");
        String publisher = employer == null ? "" : employer.optString("name", "").trim();
        if (publisher.isEmpty()) {
            // Le titre Open Graph observe commence par [Nom de l'entreprise].
            String ogTitle = document.select("meta[property=og:title]").attr("content");
            int closing = ogTitle.indexOf(']');
            if (ogTitle.startsWith("[") && closing > 1) {
                publisher = ogTitle.substring(1, closing).trim();
            }
        }

        String education = info(document, "Niveau d'étude");
        String contract = info(document, "Contrat");
        String category = info(document, "Fonctions");
        if (contract.isEmpty()) {
            contract = metadata.optString("employmentType", "").trim();
        }
        if (category.isEmpty()) {
            category = info(document, "Secteurs");
        }
        String description = String.join(" ", section(document, "Poste"));
        if (description.isEmpty()) {
            description = Jsoup.parse(metadata.optString("description", "")).text();
        }
        if (description.isEmpty()) {
            return null;
        }
        ArrayList<String> requirements = section(document, "Profil recherché");
        String experience = info(document, "Expérience");
        if (!experience.isEmpty()) {
            requirements.add("Expérience : " + experience);
        }
        String url = siteUrl(document.location());
        if (url.isEmpty()) {
            url = siteUrl(document.select("meta[property=og:url]").attr("content"));
        }
        // Job echappe deja titre/description ; arffData() exporte les autres champs bruts.
        return new Job(url, title, arffField(city), publication, requirements, description,
                publisher, arffField(education), arffField(contract), arffField(category));
    }

    private static String info(Document document, String label) {
        for (Element row : document.select(".jo-infoRow")) {
            if (row.select(".jo-infoLabel").text().equalsIgnoreCase(label)) {
                Elements values = row.select(".jo-infoValue .jo-infoItem");
                return values.isEmpty() ? row.select(".jo-infoValue").text()
                        : String.join(" ; ", values.eachText());
            }
        }
        return "";
    }

    private static ArrayList<String> section(Document document, String label) {
        ArrayList<String> paragraphs = new ArrayList<>();
        for (Element heading : document.select("#BlocDetailAnnonce h3.jo-secTitle")) {
            if (!heading.text().replace(":", "").trim().equalsIgnoreCase(label)) {
                continue;
            }
            // Les <p class=jo-posteIntro> du site contiennent d'autres <p> :
            // Jsoup les remet en freres. Parcourir jusqu'a la prochaine section.
            for (Element next = heading.nextElementSibling(); next != null;
                    next = next.nextElementSibling()) {
                if (next.hasClass("jo-blockSpacer")) {
                    continue;
                }
                if (!next.tagName().matches("p|ul|ol")) {
                    break;
                }
                Elements items = next.select("li");
                if (items.isEmpty()) {
                    if (!next.text().isEmpty()) {
                        paragraphs.add(next.text());
                    }
                } else {
                    paragraphs.addAll(items.eachText());
                }
            }
            break;
        }
        return paragraphs;
    }

    private static JSONObject jobMetadata(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                // Le HTML observe contient des retours ligne litteraux dans description.
                JSONObject object = new JSONObject(script.data().replace('\r', ' ')
                        .replace('\n', ' ').replace('\t', ' '));
                if ("JobPosting".equals(object.optString("@type"))) {
                    return object;
                }
            } catch (JSONException e) {
                // Les classes HTML restent utilisables si les metadonnees sont invalides.
            }
        }
        return new JSONObject();
    }

    private static String siteUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && ("www.rekrute.com".equalsIgnoreCase(host) || "rekrute.com".equalsIgnoreCase(host))) {
                return url.split("#", 2)[0];
            }
        } catch (IllegalArgumentException e) {
            // Un lien absent ou mal forme n'est pas une annonce exploitable.
        }
        return "";
    }

    private static String arffField(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public ArrayList<Job> jobs() {
        return new ArrayList<>(emploiDispo);
    }
}
