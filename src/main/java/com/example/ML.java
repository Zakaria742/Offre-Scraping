package com.example;

import java.util.Random;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import java.io.File;
import java.io.IOException;
import javax.annotation.processing.FilerException;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class ML {

    public ML(String arffFilePath) {
        ArffLoader loader = new ArffLoader();
        Instances data;
        try {

            loader.setFile(new File(arffFilePath));
            data = loader.getDataSet();

            data.setClassIndex(data.attribute("Categorie").index());

            Instances shuffled = new Instances(data);
            shuffled.randomize(new Random(1));
            shuffled.stratify(5);

            Instances train = shuffled.trainCV(5, 0);
            Instances test = shuffled.testCV(5, 0);

            int DescIndex = train.attribute("Description").index();
            int CategIndex = train.attribute("Categorie").index();

            StringToWordVector filter = new StringToWordVector();

            FilteredClassifier model = new FilteredClassifier();
            model.setFilter(filter);
            model.setClassifier(new NaiveBayes());

            model.buildClassifier(train);

            // #Testing

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(model, test);

            System.out.println(evaluation.toSummaryString());
            System.out.println(evaluation.toClassDetailsString());
            System.out.println(evaluation.toMatrixString());
            // #EndTesting

            model.buildClassifier(data);
            Instance testData = new DenseInstance(data.numAttributes());
            testData.setDataset(data);
            testData.setValue(data.attribute("Description"),
                    "Nous recherchons un senior competent capable d'interagir avec l'equipe specialiser dans le domaine developpement des applications et site vitrine");
            testData.setMissing(data.attribute("Categorie"));

            double predictor = model.classifyInstance(testData);
            String prediction = data.classAttribute().value((int) predictor);

            System.out.println("The predicted category is : " + prediction);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}