package com.example.marocAnnonces;

import com.example.Job;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Site : https://www.marocannonces.com/categorie/309/Emploi/Offres-emploi.html
// HTML des listes et fiches examine le 18/10/2026.
// Si Java signale PKIX sous Windows, ajouter ces options VM (certificats Windows) :
// -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE
public class marocAnnonces {
    private static final String siteLink = "https://www.marocannonces.com/categorie/309/Emploi/Offres-emploi.html";
    private final ArrayList<Job> emploiDispo = new ArrayList<>();
    private long lastRequest;

    // Meme utilisation que jobPortal : new marocAnnonces(true).jobs().
    public marocAnnonces(boolean watch) {
        this(watch, 2, Integer.MAX_VALUE);
    }

    // Limites configurables ; par exemple (true, 1, 3) pour trois offres.
    public marocAnnonces(boolean watch, int maxPages, int maxJobs) {
        if (maxPages < 1 || maxJobs < 1) {
            throw new IllegalArgumentException("maxPages et maxJobs doivent etre positifs.");
        }
        Set<String> pagesVisitees = new HashSet<>();
        Set<String> annoncesVisitees = new HashSet<>();
        String pageLink = siteLink;

        for (int currentPage = 1; currentPage <= maxPages
                && !pageLink.isEmpty() && pagesVisitees.add(pageLink); currentPage++) {
            Document portail;
            try {
                portail = fetch(pageLink);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                System.err.println("MarocAnnonces : page inaccessible " + pageLink + " : " + e.getMessage());
                return;
            }

            // Les annonces sponsorisees sont hors de cette liste et peuvent se repeter.
            Elements emplois = portail.select("ul.cars-list > li a[href*='/annonce/']");
            if (emplois.isEmpty()) {
                System.err.println("MarocAnnonces : aucune offre reconnue sur " + pageLink);
                return;
            }
            for (Element emploi : emplois) {
                // absUrl tient compte du <base href> present dans le HTML.
                String link = emploi.absUrl("href");
                if (!isSiteUrl(link) || !annoncesVisitees.add(link)) {
                    continue;
                }
                try {
                    Job newJob = parseJob(fetch(link));
                    emploiDispo.add(newJob);
                    if (watch) {
                        System.out.printf("MarocAnnonces | Page %d | Offres : %d | %s%n",
                                currentPage, emploiDispo.size(), newJob.getTitle());
                    }
                    if (emploiDispo.size() >= maxJobs) {
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (HttpStatusException e) {
                    System.err.println("MarocAnnonces : HTTP " + e.getStatusCode() + " : " + link);
                    if (e.getStatusCode() == 403 || e.getStatusCode() == 429) {
                        return;
                    }
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("MarocAnnonces : offre ignoree " + link + " : " + e.getMessage());
                }
            }
            Element suivant = portail.selectFirst(".contentpaging li.pagina_suivant a[href]");
            pageLink = suivant == null ? "" : suivant.absUrl("href");
            if (!isSiteUrl(pageLink)) {
                return;
            }
        }
    }

    static Job parseJob(Document annonce) {
        Element contenu = annonce.selectFirst(".description.desccatemploi");
        if (contenu == null) {
            throw new IllegalArgumentException("Bloc .description.desccatemploi absent.");
        }
        String title = contenu.select("h1").text();
        Element bloc = contenu.selectFirst(".block");
        if (title.isBlank() || bloc == null) {
            throw new IllegalArgumentException("Titre ou description absent.");
        }
        Element description = bloc.clone();
        description.select(".box1, script, style, ins").remove();
        if (description.text().isBlank()) {
            throw new IllegalArgumentException("Description vide.");
        }

        // Lire les libelles evite de dependre de l'ordre des champs facultatifs.
        Map<String, String> infos = new LinkedHashMap<>();
        for (Element champ : contenu.select("#extraQuestionName > li")) {
            String texte = champ.text();
            int separateur = texte.indexOf(':');
            if (separateur >= 0) {
                infos.put(texte.substring(0, separateur).trim().replace('\u2019', '\''),
                        texte.substring(separateur + 1).trim());
            }
        }
        String emplacement = infos.getOrDefault("Ville", "");
        if (emplacement.isBlank()) {
            emplacement = contenu.select(".info-holder li a").text();
        }
        String datePublication = "";
        for (Element info : contenu.select(".info-holder li")) {
            if (info.text().startsWith("Publiée le")) {
                int separateur = info.text().indexOf(':');
                if (separateur >= 0) {
                    datePublication = info.text().substring(separateur + 1).trim();
                }
                break;
            }
        }
        String publisher = infos.getOrDefault("Entreprise", "");
        if (publisher.isBlank()) {
            publisher = contenu.select(".infoannonce dd").text();
        }
        String niveauEtude = infos.getOrDefault("Niveau d'études", "");
        String typeContrat = infos.getOrDefault("Contrat", "");
        String categorie = infos.getOrDefault("Domaine", "");
        if (categorie.isBlank()) {
            categorie = infos.getOrDefault("Fonction", "");
        }
        ArrayList<String> exigences = new ArrayList<>();
        if (!niveauEtude.isBlank()) {
            exigences.add("Niveau d'études : " + niveauEtude);
        }
        // Les autres exigences restent dans la description, sans les inventer.
        // Job echappe deja Title et Description ; ces quatre champs sont exportes bruts
        // par arffData().
        return new Job(annonce.location(), title, arffField(emplacement), datePublication,
                exigences, description.text(), publisher, arffField(niveauEtude),
                arffField(typeContrat), arffField(categorie));
    }

    private Document fetch(String url) throws IOException, InterruptedException {
        long pause = 1000L - (System.currentTimeMillis() - lastRequest);
        if (pause > 0) {
            Thread.sleep(pause);
        }
        try {
            return Jsoup.connect(url)
                    .userAgent("Offre-Scraping/1.0 (Java; Jsoup)")
                    .timeout(30000)
                    .maxBodySize(0)
                    .get();
        } finally {
            lastRequest = System.currentTimeMillis();
        }
    }

    private static boolean isSiteUrl(String url) {
        try {
            URI uri = URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && ("www.marocannonces.com".equalsIgnoreCase(uri.getHost())
                            || "marocannonces.com".equalsIgnoreCase(uri.getHost()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String arffField(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public ArrayList<Job> jobs() {
        return emploiDispo;
    }
}
