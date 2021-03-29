package com.company;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ID3 {
    private List<Integer> learners = new ArrayList<>();
    private List<List<String>> dataset;
    private TreeNode root;

    public ID3(List<List<String>> dataset) {
        this.dataset = dataset;
        root = new TreeNode("", 0);
    }

    public void addLearner(List<Integer> toAdd) {
        learners.addAll(toAdd);
    }

    public TreeNode getRoot() {
        return root;
    }

    public double entropy(List<List<String>> dataset, int positiveRows, int allRows) {
        double prop = 0;

        if (positiveRows == 0) {
            positiveRows = countPositive(dataset);

            prop = (double) positiveRows / dataset.size();
        } else prop = (double) positiveRows / allRows;

        if (prop == 0 || prop == 1)
            return 0;
        return -(prop * log2(prop) + (1 - prop) * log2(1 - prop));
    }

    public double informationGain(List<List<String>> dataset, int attributeIndex) {
        HashMap<String, int[]> allValues = new HashMap<>();

        for (List<String> elem : dataset) {
            String attrValue = elem.get(attributeIndex);

            if (!attrValue.equals("?")) {
                int[] positive_allOccur = new int[2];

                if (allValues.containsKey(attrValue))
                    positive_allOccur = allValues.get(attrValue);

                positive_allOccur[1]++;

                if (elem.get(0).equals("recurrence-events"))
                    positive_allOccur[0]++;

                allValues.put(attrValue, positive_allOccur);
            }
        }

        double sum = 0;

        for (String attrValue : allValues.keySet()) {
            int[] occurrences = allValues.get(attrValue);

            sum += (double) (occurrences[1] / dataset.size()) * entropy(dataset, occurrences[0], occurrences[1]);
        }

        double entr = entropy(dataset, 0, 0);
        return entr - sum;
    }

    public int maxInformationGainAttr(List<List<String>> dataset, List<Integer> attrIndexes) {
        double maxGain = 0;
        int maxIndex = 0;

        for (int i = 0; i < attrIndexes.size(); i++) {
            double currGain = informationGain(dataset, attrIndexes.get(i));

            if (currGain > maxGain) {
                maxGain = currGain;
                maxIndex = i;
            }
        }
        return attrIndexes.get(maxIndex);
    }

    private List<String> attrValues(int attributeIndex) {
        List<String> list = new ArrayList<>();

        for (List<String> elem : dataset)
            if (!elem.get(attributeIndex).equals("?") && !list.contains(elem.get(attributeIndex)))
                list.add(elem.get(attributeIndex));

        return list;
    }

    private int countPositive(List<List<String>> dataset) {
        int positiveRows = 0;

        for (List<String> strings : dataset)
            if (strings.get(0).equals("recurrence-events"))
                positiveRows++;

        return positiveRows;
    }

    public void buildTree(List<List<String>> dataset, TreeNode currRoot, List<Integer> attributeIndexes) {
        if (attributeIndexes.size() == 0)
            return;


        double rootEntropy = entropy(dataset, 0, 0);

        if (rootEntropy == 0)
            currRoot.children.add(new TreeNode(dataset.get(0).get(0), currRoot.parentIndex));


        else {
            int maxGainIndex = maxInformationGainAttr(dataset, attributeIndexes);
            attributeIndexes.remove(Integer.valueOf(maxGainIndex));

            if (currRoot.parentIndex == 0) {
                currRoot.data = String.valueOf(maxGainIndex);
                currRoot.children = new ArrayList<>();
            }

            for (String attr : attrValues(maxGainIndex))
                currRoot.children.add(new TreeNode(attr, maxGainIndex));

            for (TreeNode child : currRoot.children) {
                List<List<String>> divideDataset = new ArrayList<>();

                for (List<String> row : dataset)
                    if (row.get(maxGainIndex).equals(child.data))
                        divideDataset.add(row);

                List<Integer> tmpList = new ArrayList<>(attributeIndexes);
                if (divideDataset.size() == 0) {

                    if (countPositive(dataset) >= dataset.size() / 2)
                        child.children.add(new TreeNode("recurrence-events", child.parentIndex));
                    else child.children.add(new TreeNode("no-recurrence-events", child.parentIndex));
                } else buildTree(divideDataset, child, tmpList);
            }
        }
    }

    public String decision(List<String> element, TreeNode currRoot) {
        TreeNode tmp = currRoot;

        if (tmp.children.size() == 0)
            return tmp.data;

        else if (tmp.children.size() > 1)
            for (TreeNode branch : tmp.children)
                if (element.get(tmp.parentIndex).equals(branch.data))
                    return decision(element, branch);

        return decision(element, tmp.children.get(0));
    }

    public void setData() {
        List<List<String>> newData = new ArrayList<>();

        for (Integer learner : learners)
            newData.add(dataset.get(learner));

        dataset = newData;
    }


    public int errorEstimate(TreeNode currRoot, List<List<String>> validationSet) {
        if (currRoot.children.size() == 0)
            return 0;


        int error = 0;

        for (List<String> testRow : validationSet) {
            if (!decision(testRow, currRoot).equals(testRow.get(0)))
                error++;
        }

        return error;
    }

    public void tryToPrune(TreeNode currRoot) {
        if (currRoot.children.size() == 1)
            return;

        List<List<String>> validationSet = new ArrayList<List<String>>(dataset);
        Collections.shuffle(validationSet);
        validationSet = validationSet.subList(0, validationSet.size() / 3);

        for (TreeNode child : currRoot.children) {
            int posErr = 0, negativeErr = 0;
            TreeNode tmp = new TreeNode(currRoot);
            findAndReplaceNode("recurrence-events",child.data,tmp);
            posErr = errorEstimate(tmp,validationSet);
            findAndReplaceNode("no-recurrence-events",child.data,tmp);
            negativeErr = errorEstimate(tmp,validationSet);

            if(errorEstimate(currRoot,validationSet) >= Math.min(posErr,negativeErr)) {
                if (posErr > negativeErr)
                    findAndReplaceNode("no-recurrence-events", child.data, currRoot);
                else findAndReplaceNode("recurrence-events", child.data, currRoot);
            }
            else tryToPrune(child);
        }

    }


    public void findAndReplaceNode(String className, String data, TreeNode node) {

        if (node.data.equals(data)) {
            node.children.clear();
            node.children.add(new TreeNode(className, node.parentIndex));
        } else for (TreeNode child : node.children)
            findAndReplaceNode(className, data, child);
    }

    public List<List<String>> getDataset() {
        return dataset;
    }

    private double log2(double N) {
        return Math.log(N) / Math.log(2);
    }

    private class TreeNode {
        List<TreeNode> children;
        String data;
        int parentIndex;
        int errorCount;

        public TreeNode(String data, int parentIndex) {
            this.children = new ArrayList<>();
            this.data = data;
            this.parentIndex = parentIndex;
            this.errorCount = 0;
        }

        public TreeNode(TreeNode other) {
            this.children = other.children;
            this.data = other.data;
            this.parentIndex = other.parentIndex;
            this.errorCount = other.errorCount;
        }
    }
}
