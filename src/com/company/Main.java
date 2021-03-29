package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File f = new File("src/com/company/breast-cancer.data");

        Scanner sc = new Scanner(f);

        List<List<String>> list = new ArrayList<>();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            list.add(Arrays.asList(line.split(",")));
        }

        List<Integer> shuffled = IntStream.range(0, 286)
                .boxed()
                .collect(Collectors.toList());

        Collections.shuffle(shuffled);

        List<List<Integer>> sets = new ArrayList<>(10);

        for (int i = 0; i < shuffled.size(); i += 28) {
            if (sets.size() < 9)
                sets.add(shuffled.subList(i, i + 28));
            else sets.add(shuffled.subList(i, shuffled.size()));
        }

        double avgAccuracy = 0.0;

        List<Integer> attributeIndexes = IntStream.range(1, 10)
                .boxed()
                .collect(Collectors.toList());

        for (int i = 0; i < 10; i++) {
            int correctCount = 0;
            ID3 tree = new ID3(list);

            for (int j = 0; j < 10; j++)
                if (i != j)
                    tree.addLearner(sets.get(j));


            List<Integer> tmpList = new ArrayList<>(attributeIndexes);
            tree.setData();
            tree.buildTree(tree.getDataset(), tree.getRoot(), tmpList);
            tree.tryToPrune(tree.getRoot());


            for (int testIndex : sets.get(i)) {
                if (tree.decision(list.get(testIndex), tree.getRoot()).equals(list.get(testIndex).get(0)))
                    correctCount++;

            }

            double accuracy = (correctCount / (double) sets.get(i).size()) * 100;
            avgAccuracy += accuracy;
            System.out.printf("Study %d: %.2f%%\n", i + 1, accuracy);
        }

        System.out.printf("Average accuracy is %.2f%%\n", avgAccuracy / 10);

    }
}
