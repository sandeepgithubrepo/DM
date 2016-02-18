package com.datamining.algo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Changes : Added candidate pruning based on support count
 * Class for Apriori algorithm implementation and data set analysis
 * @author sandeep
 */

public class AprioriImpl {
	public static double MINSUP = 0.05;
	public static String FILEPATH = "/home/sandeep/DM/data-2016.csv";
	public static int TOTAL_TRANS;

	/**
	 * Utility method to read File
	 * @param filePath :Specify absolute file path
	 * @return Stream<String>
	 */
	public static Stream<String> readFile(String filePath) {
		Stream<String> lines = null;
		try {
			lines = Files.lines(Paths.get(filePath));
		} catch (IOException e) {
			System.err.println("File not found at" + filePath);
		}
		return lines;
	}

	/**
	 * Utility method for course ID extraction
	 * @param lines
	 * @return {@link ArrayList<Integer>}
	 */
	public static ArrayList<Integer> getCourseIdList(Stream<String> lines) {
		ArrayList<Integer> courses = new ArrayList();
		lines.forEach(line -> {
			String[] columns = line.split(" ");
			for (int i = 2; i < columns.length; i += 5) {
				int cid = Integer.parseInt(columns[i]);
				if (!courses.contains(cid)) {
					courses.add(cid);

				}
			}
		});
		TOTAL_TRANS = courses.size();
		return courses;
	}

	/**
	 * Generate One frequent item set Sort in order
	 * @param courseIdList
	 * @param transactionsMap
	 * @return ArrayList<ArrayList<Integer>>
	 */
	public static ArrayList<ArrayList<Integer>> getOneItemset(ArrayList<Integer> courseIdList,
			HashMap<Integer, ArrayList<ArrayList<Integer>>> transactionsMap) {

		ArrayList<ArrayList<Integer>> oneItemsets = new ArrayList();
		for (int id : courseIdList) {
			ArrayList<Integer> item = new ArrayList();
			item.add(id);
			if (calculateSupport(item, transactionsMap) >= MINSUP) {
				oneItemsets.add(item);
			}
		}

		Comparator<ArrayList<Integer>> comparator = (ArrayList<Integer> o1, ArrayList<Integer> o2) -> {
			for (int i = 0; i < o1.size(); i++) {
				if (o1.get(i) > o2.get(i)) {
					return 1;
				} else if (o2.get(i) > o1.get(i)) {
					return -1;
				}
			}
			return 0;
		};

		Collections.sort(oneItemsets, comparator);
		return oneItemsets;
	}

	/**
	 * This method creates transactions from the given data set
	 * @param lines {@link Stream<String>}
	 * @return {@link ArrayList<ArrayList<Integer>>}
	 */
	public static ArrayList<ArrayList<Integer>> createTransactions(Stream<String> lines) {
		Stream<Student> studentStream = lines.map(Student::createStudent);
		ArrayList<ArrayList<Integer>> transactions = new ArrayList();
		studentStream.forEach(s -> {
			ArrayList<Integer> courseCodes = new ArrayList();
			for (CourseTranscript ct : s.courseTranscriptList) {
				courseCodes.add(ct.courseCode);
			}
			transactions.add(courseCodes);
		});

		return transactions;
	}

	/**
	 * Initialize the support for all item sets in the transaction
	 * 
	 * @param transactions
	 * @param courses
	 * @return
	 */
	public static HashMap<Integer, ArrayList<ArrayList<Integer>>> initSupportForTransaction(
			ArrayList<ArrayList<Integer>> transactions, ArrayList<Integer> courses) {
		HashMap<Integer, ArrayList<ArrayList<Integer>>> map = new HashMap();

		for (int id : courses) {
			ArrayList<ArrayList<Integer>> idTransactions = new ArrayList();
			for (ArrayList<Integer> t : transactions) {
				if (t.contains(id)) {
					idTransactions.add(t);
				}
			}
			map.put(id, idTransactions);
		}
		return map;
	}

	/**
	 * This method calculated Support for the given candidate and transactions
	 * 
	 * @param candidate
	 * @param transactions
	 * @return {@link Double}
	 */
	public static double calculateSupport(ArrayList<Integer> candidate,
			HashMap<Integer, ArrayList<ArrayList<Integer>>> transactions) {
		if (candidate.size() == 1) {
			ArrayList<ArrayList<Integer>> candidateItem = transactions.get(candidate.get(0));
			return (double) candidateItem.size()/TOTAL_TRANS;
		}

		ArrayList<ArrayList<Integer>> firstCandidate = (ArrayList<ArrayList<Integer>>) transactions
				.get(candidate.get(0)).clone();
		for (int i = 1; i < candidate.size(); i++) {
			ArrayList<ArrayList<Integer>> candidiateEach = transactions.get(candidate.get(i));
			firstCandidate.retainAll(candidiateEach);
			if (firstCandidate.size() <= 0) {
				return 0.0;
			}
		}

		return (double) firstCandidate.size()/TOTAL_TRANS;
	}

	/**
	 * Find frequent Item sets
	 * 
	 * @param itemsets
	 * @param transactions
	 */
	public static void calculateFrequentItemsets(ArrayList<ArrayList<Integer>> itemsets,
			HashMap<Integer, ArrayList<ArrayList<Integer>>> transactions) {
		if (itemsets.size() < 1) {
			return;
		}

		int candidateCount = Integer.MAX_VALUE;
		while (candidateCount > 0) {
			int itemsetSize = itemsets.get(0).size() + 1;
			ArrayList<ArrayList<Integer>> candidates = generate(itemsets);
			ArrayList<ArrayList<Integer>> newCandidates = new ArrayList();
			for (ArrayList<Integer> candidate : candidates) {
				if (!checkPruneStatus(candidate, itemsets)) {
					double support = calculateSupport(candidate, transactions);
					if (support >= MINSUP) {
						newCandidates.add(candidate);
					}
				}
			}
			candidateCount = newCandidates.size();
			itemsets = newCandidates;
		}
	}

	/**
	 * Check candidate is pruned or not
	 * 
	 * @param candidate
	 * @param itemsets
	 * @return
	 */
	public static boolean checkPruneStatus(ArrayList<Integer> candidate, ArrayList<ArrayList<Integer>> itemsets) {
		Comparator<ArrayList<Integer>> comparator = (ArrayList<Integer> o1, ArrayList<Integer> o2) -> {
			for (int i = 0; i < o1.size(); i++) {
				if (o1.get(i) > o2.get(i)) {
					return 1;
				} else if (o2.get(i) > o1.get(i)) {
					return -1;
				}
			}
			return 0;
		};
		for (int i = 0; i < candidate.size(); i++) {
			ArrayList<Integer> itemsubSet = new ArrayList();
			for (int j = 0; j < candidate.size(); j++) {
				if (i != j) {
					itemsubSet.add(candidate.get(j));
				}
			}
			int index = Collections.binarySearch(itemsets, itemsubSet, comparator);
			if (index < 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Merges given two item sets
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static ArrayList<Integer> merge(ArrayList<Integer> list1, ArrayList<Integer> list2) {
		Set<Integer> union = new TreeSet<Integer>();
		union.addAll(list1);
		union.addAll(list2);
		return new ArrayList<Integer>(union);
	}

	/**
	 * Generate all candidates
	 * 
	 * @param itemSet
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> generate(ArrayList<ArrayList<Integer>> itemSet) {
		ArrayList<ArrayList<Integer>> response = new ArrayList();
		if (itemSet.size() <= 1) {
			return response;
		}
		int prev = 0;
		while (prev < itemSet.size() - 1) {
			ArrayList<Integer> prevList = itemSet.get(prev);
			int cur = prev + 1;
			while (cur < itemSet.size()) {
				ArrayList<Integer> curList = itemSet.get(cur);
				if (prevList.size() == 1 || curList.size() == 1) {
					response.add(merge(prevList, curList));
				} else {
					if (prevList.subList(0, prevList.size() - 1).equals(curList.subList(0, curList.size() - 1))) {
						response.add(merge(prevList, curList));
					} 
				}
				cur++;
			}
			prev++;
		}
		return response;
	}
	public static class Student {
		int startingYear;
		ArrayList<CourseTranscript> courseTranscriptList;
		static Student createStudent(String line) {
			String[] columns = line.split(" ");
			Student s = new Student();
			s.startingYear = Integer.parseInt(columns[0]);
			s.courseTranscriptList = CourseTranscript.createCourseTranscriptList(line);
			return s;
		}
	}
	public static class CourseTranscript {
		int startingMonth;
		String courseName;
		int courseCode;
		int grade;
		double creditPoint;
		
		static ArrayList<CourseTranscript> createCourseTranscriptList(String line) {
			String[] columns = line.split(" ");
			ArrayList<CourseTranscript> courseTranscriptList = new ArrayList<>();
			for (int i = 1; i + 4 < columns.length; i += 5) {
				CourseTranscript c = new CourseTranscript();
				c.startingMonth = Integer.parseInt(columns[i].replace("-", ""));
				c.courseCode = Integer.parseInt(columns[i + 1]);
				c.courseName = columns[i + 2];
				c.creditPoint = Double.parseDouble(columns[i + 3]);
				c.grade = Integer.parseInt(columns[i + 4]);
				courseTranscriptList.add(c);
			}
			return courseTranscriptList;
		}
	}
	
	public static void main(String[] args) {
		
		Stream<String> lines = readFile(FILEPATH);
		ArrayList<Integer> courseIdList = getCourseIdList(lines);
		lines = readFile(FILEPATH);
		ArrayList<ArrayList<Integer>> transactions = createTransactions(lines);
		HashMap<Integer, ArrayList<ArrayList<Integer>>> transactionSupportTrackMap = initSupportForTransaction(
				transactions, courseIdList);
		ArrayList<ArrayList<Integer>> oneItemsets = getOneItemset(courseIdList, transactionSupportTrackMap);
		calculateFrequentItemsets(oneItemsets, transactionSupportTrackMap);
	}
}