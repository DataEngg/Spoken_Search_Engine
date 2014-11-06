package org.galagosearch.core.tools;

import java.util.TreeMap;

public class Combination {
	TreeMap<Integer, String> permute_map;// = new TreeMap<Integer, String>();
	int permute_count;

	public TreeMap<Integer, String> combine(String[] args) {
		permute_map = new TreeMap<Integer, String>();
		permute_count = 0;
		// int [] arr = {1, 2, 3, 4, 5};
		// String[] input = { "jeevan", "joishi", "president", "iiitd" };
		int n = args.length;
		for (int count = 2; count <= n; ++count) {
			int r = count;
			// int n = arr.length;

			// printCombination(arr, n, r);
			printCombination(args, n, r);
		}
		return permute_map;
	}

	private void printCombination(String[] arr, int n, int r) {
		// int []data=new int[r];
		String[] data = new String[r];
        combinationUtil(arr, data, 0, n - 1, 0, r);

	}

	private void combinationUtil(String[] arr, String[] data, int start,
			int end, int index, int r) {
		if (index == r) {
			permute_count++;
			String insert = "";
			for (int j = 0; j < r; j++)
				insert += data[j] + " ";
			insert = insert.trim();
			permute_map.put(permute_count, insert);
			return;
		}
		for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
			data[index] = arr[i];
			combinationUtil(arr, data, i + 1, end, index + 1, r);
		}

	}
}