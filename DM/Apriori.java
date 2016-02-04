package com.data.mining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Class for 
 * Apriori algorithm implementation
 * and data set analysis
 * 
 * @author sandeep
 *
 */
public class Apriori {
	public static String FILEPATH = "data-2016.csv";
	public static double MINSUP = 0.1;

	/**
	 * Utility method to read File 
	 * @param filePath :Specify absolute file path
	 * @return  Stream<String> 
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
	 * create one frequent item sets
	 * 
	 * @param lines
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> createOneItemSets(Stream<String> lines) {
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

		ArrayList<ArrayList<Integer>> oneItemsets = new ArrayList();
		System.out.println("Total courses are\t" + courses.size());

		for (int course : courses) {
			ArrayList<Integer> item = new ArrayList<Integer>();
			item.add(course);
			oneItemsets.add(item);
		}

		return oneItemsets;
	}

	/**
	 * This method creates transactions from the given
	 * data set
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
	 * This method calculates Frequent item sets
	 * 
	 * @param itemsets {@link ArrayList<ArrayList<Integer>>}
	 * @param transactions { {@link ArrayList<ArrayList<Integer>>}
	 */
	public static void calculateFrequentItemsets(ArrayList<ArrayList<Integer>> itemsets,
			ArrayList<ArrayList<Integer>> transactions) {
		if (itemsets.size() < 1) {
			return;
		}
		int candidateCount = Integer.MAX_VALUE;
		while (candidateCount > 0) {
			
			ArrayList<ArrayList<Integer>> candidates = generate(itemsets, transactions);
			ArrayList<ArrayList<Integer>> nextCandidates = new ArrayList();
			for (ArrayList<Integer> candidate : candidates) {
				double support = support(transactions, candidate);

				if (support > MINSUP) {
					nextCandidates.add(candidate);
					//System.out.println("item set: " + candidate + ", minSup: " + support);
				}
			}
			System.out.println("Total number of item sets: " + nextCandidates.size());
			candidateCount = nextCandidates.size();
			itemsets = nextCandidates;
		}
	}

	/**
	 * This method calculates support for the given item set
	 * 
	 * @param transactions
	 * @param itemset
	 * @return {@link double}
	 */
	public static double support(ArrayList<ArrayList<Integer>> transactions, ArrayList<Integer> itemset) {
		int supportCount = 0;
		for (ArrayList<Integer> transaction : transactions) {
			int count = 0;
			for (int item : itemset) {
				if (transaction.contains(item)) {
					count++;
				}
			}
			if (count == itemset.size()) {
				supportCount++;
			}
		}
		return (double) supportCount / transactions.size();
	}

	/**
	 * This method creates union of two item sets
	 * @param list1 {@link ArrayList<Integer>}
	 * @param list2 {@link ArrayList<Integer>}
	 * @return
	 */
	public static ArrayList<Integer> merge(ArrayList<Integer> list1, ArrayList<Integer> list2) {
		ArrayList<Integer> unionList = new ArrayList();

		for (int item : list1) {
			unionList.add(item);
		}

		for (int item : list2) {
			if (!unionList.contains(item)) {
				unionList.add(item);
			}
		}
		return unionList;
	}

	/**
	 * This method generates item set of size k using item sets of size k-1
	 * @param lists
	 * @param transactions
	 * @return {@link ArrayList<ArrayList<Integer>>}
	 */
	public static ArrayList<ArrayList<Integer>> generate(ArrayList<ArrayList<Integer>> lists,
			ArrayList<ArrayList<Integer>> transactions) {
		ArrayList<ArrayList<Integer>> response = new ArrayList();

		if (lists.size() <= 1) {
			return response;
		}

		int prev = 0;
		while (prev < lists.size() - 1) {
			ArrayList<Integer> prevList = lists.get(prev);
			if (support(transactions, prevList) > MINSUP) {
				int cur = prev + 1;
				while (cur < lists.size()) {
					ArrayList<Integer> curList = lists.get(cur);

					if (prevList.size() == 1 || curList.size() == 1) {
						if (support(transactions, curList) > MINSUP) {
							response.add(merge(prevList, curList));
						}
					} else {
						if (prevList.subList(0, prevList.size() - 1).equals(curList.subList(0, curList.size() - 1))) {
							if (support(transactions, curList) > MINSUP) {
								response.add(merge(prevList, curList));
							}
						}
					}
					cur++;
				}
			}
			prev++;
		}
		return response;
	}

	/**
	 * Utility class for data set to hold student records (transactions)
	 * @author sandeep
	 *
	 */
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

	/**
	 * Utility class for Data set to hold student course details
	 * @author sandeep
	 *
	 */
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

		public String getCourseName() {
			return this.courseName;
		}
	}

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {

		Stream<String> lines = readFile(FILEPATH);
		ArrayList<ArrayList<Integer>> oneItemsets = createOneItemSets(lines);
		lines = readFile(FILEPATH);
		ArrayList<ArrayList<Integer>> transactions = createTransactions(lines);
		long s = System.nanoTime();
		calculateFrequentItemsets(oneItemsets, transactions);
		long end = System.nanoTime();
		System.out.println("time\t"+(end-s));
	}

}
