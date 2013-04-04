package edu.upenn.cis.cis121.hw5;

/**
 * Class for MovieFinder application
 * Reads in a database of movies, takes queries for searching/sorting,
 * and returns a list of movies in sorted order
 *
 * @author Justin Warner (wjustin@wharton.upenn.edu)
 * 		   CIS 121, Spring 2012
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
 
public class MovieFinder {
	private ArrayList<Movie> _movies;	
	
	/**
	 * Constructor
	 */
	public MovieFinder() {
		_movies = new ArrayList<Movie>();
	}
	
	/**
	 * Accessor.
	 * @return list of movies
	 */
	public ArrayList<Movie> getAllMovies() {
		return _movies;
	}
	
	/**
	 * Takes the name of a comma-separated (.csv) text file as input and updates
	 * _movies to include the entire database of movies.  Each line consists of 
	 * data for a single movie in the following order: Title, Rating, Votes, Awards.
	 * The first line of the file consists only of headers and should be skipped.
	 * @param in_file
	 * @throws IOException 
	 */
	public void readMovies(String inFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inFile));
		//skip first line
		in.readLine();
		while (in.ready()) {
			String info = in.readLine();
			String[] movieParts = info.split(",");
			Movie toAdd = new Movie(movieParts[0], Double.parseDouble(movieParts[1]), 
					Integer.parseInt(movieParts[2]), Integer.parseInt(movieParts[3]));
			_movies.add(toAdd);
		}
		in.close();
	}
	
	/**
	 * Takes a MovieQuery object as input and returns an ArrayList<Movie> of 
	 * movies resulting from the query.  The movies should also be sorted in
	 * the order specified by the query.
	 * @param query
	 * @return list of movies satisfying query in the order specified by query
	 */
	public ArrayList<Movie> queryMovies(MovieQuery query) {
		
		if (query == null) {
			throw new IllegalArgumentException("Input query is null.");
		}
		
		String[] searchParams = query.searchParams();
		String[] searchOperators = query.searchOperators();
		double[] searchOperatorValues = query.searchOperatorVals();
		String[] sortParams = query.sortParams();
		
		//checking validity of inputs
		if (sortParams.length > 3) {
			throw new IllegalArgumentException("Sort params is invalid.");
		}
		
		if (searchParams.length != searchOperators.length ||
				searchParams.length != searchOperatorValues.length) {
			throw new IllegalArgumentException("Search params are invalid.");
		}
		
		for (int i = 0; i < searchParams.length; i++) {
			if (searchParams[i] != MovieQuery.RATING &&  
					searchParams[i] != MovieQuery.VOTES && 
					searchParams[i] != MovieQuery.AWARDS) {
				throw new IllegalArgumentException("Invalid inputs in searchParams.");
			}
		}
		
		for (int i = 0; i < searchOperators.length; i++) {
			if (searchOperators[i] != MovieQuery.AT_MOST &&  
					searchOperators[i] != MovieQuery.AT_LEAST &&
					searchOperators[i] != MovieQuery.EQUAL_TO) {
				throw new IllegalArgumentException("Invalid inputs in searchOperators.");
			}
		}
		
		ArrayList<Movie> result = _movies;

		//filter out the results according to the search parameters
		for (int i = 0; i < searchParams.length; i++) {
			result = filterSearch(query, searchParams[i], i, result);
		}

		//sort the list using a modified version of insertion sort
		for (int i = 1; i < result.size(); i++) {
			int x = i;
			while (x > 0 && toSwap(query, 0, result.get(x-1), result.get(x))) {
				Movie temp = result.get(x);
				result.set(x, result.get(x - 1));
				result.set(x - 1, temp);
				x--;
			}
		}
		
		return result;
	}
	
	/**
	 * Takes a MovieQuery object as input and returns a boolean value to 
	 * determine if the two movies being compared should have their positioned 
	 * swapped. 
	 * @param query - contains all relevant searching and sorting information
	 * @param n - current index of the search
	 * @param current - one of the movies being compared
	 * @param old - other movie being compared
	 * @return boolean value depending on whether to swap the positions of the 
	 * movies or not
	 */
	private boolean toSwap(MovieQuery query, int n, Movie old, Movie current) {
		
		String[] sortParams = query.sortParams();
		boolean output = false;
		
		//return false if no more parameters to compare with after traversing 
		//through all the options (i.e. don't swap)
		if (n >= query.sortParams().length) { 
			return false;
		}
		
		//swap if in wrong order
		//if equal, compare with next sorting parameter recursively
		if (sortParams[n].equals(MovieQuery.AWARDS)) {
			if (current.getAwards() > old.getAwards()) {
				output = true;
			}
			else if (current.getAwards() == old.getAwards()) {
				output = toSwap(query, n + 1, old, current);
			}
		}

		else if (sortParams[n].equals(MovieQuery.RATING)) {
			if (current.getRating() > old.getRating()) {
				output = true;
			}
			else if (current.getRating() == old.getRating()) {
				output = toSwap(query, n + 1, old, current);
			}
		}

		else if (sortParams[n].equals(MovieQuery.VOTES)) {
			if (current.getVotes() > old.getVotes()) {
				output = true;
			}
			else if (current.getVotes() == old.getVotes()) {
				output = toSwap(query, n + 1, old, current);
			}
		}
		return output; 
	}

	/**
	 * Takes in a MovieQuery and returns an ArrayList of movies that are 
	 * filtered according to the current searchParam.
	 * @param query - contains all relevant searching and sorting information
	 * @param type - the type of "rating" being used
	 * @param count - the index of the parameter
	 * @param list - list of all the movies
	 * @return list of movies satisfying query in the order specified by query
	 */

	private ArrayList<Movie> filterSearch(MovieQuery query, String type, 
			int count, ArrayList<Movie> list) {

		String[] searchOperators = query.searchOperators();
		ArrayList<Movie> returnList = new ArrayList<Movie>();
		Iterator<Movie> itr = list.iterator();

		while (itr.hasNext()) {

			Movie current = itr.next();
			double compare = 0.0;
			
			//gets the value of the appropriate rating of the current movie
			if (type.equals(MovieQuery.RATING)) {
				compare = current.getRating();
			}
			else if (type.equals(MovieQuery.AWARDS)) {
				compare = current.getAwards(); 
			}
			else if (type.equals(MovieQuery.VOTES)) {
				compare = current.getVotes(); 
			}

			//checks to see if it should be added into the list
			if (searchOperators[count].equals(MovieQuery.AT_LEAST)) {
				if (compare >= query.searchOperatorVals()[count]) {
					returnList.add(current);
				}
			}
			else if (searchOperators[count].equals(MovieQuery.AT_MOST)) {
				if (compare <= query.searchOperatorVals()[count]) {
					returnList.add(current);
				}
			}
			else if (searchOperators[count].equals(MovieQuery.EQUAL_TO)) {
				if (compare == query.searchOperatorVals()[count]) {
					returnList.add(current);
				}
			}
		}

		return returnList;
	}
	
	/**
	 * Takes a MovieQuery object as input and returns a two-dimensional array of 
	 * Strings resulting from the query.  Each element of the first dimension of
	 * the array corresponds to a movie resulting from the query while each element
	 * of the second dimension of the array corresponds to a field being selected.
	 * @param query
	 * @return list of selected parameters from the movies satisfying query
	 in the order specified by query
	 */
	public String[][] queryMoviesSelect(MovieQuery query) {
		
		
		String[] selectParams = query.selectParams();
		String[][] returnArray;
		//filter movies
		ArrayList<Movie> filtered = queryMovies(query);
		int first = filtered.size();
		
		//filters the list of selectParameters
		ArrayList<String> check = new ArrayList<String>();
		for (int i = 0; i < selectParams.length; i++) {
			if (selectParams[i].equals(MovieQuery.AWARDS)
					|| selectParams[i].equals(MovieQuery.VOTES)
					|| selectParams[i].equals(MovieQuery.TITLE)) {
				check.add(selectParams[i]);
			}
		}
		int second = check.size();
		
		//construct new string[][] as appropriate
		if (second != 0) {
			returnArray = new String[first][second];	
		}
		else {
			return returnArray = new String[0][0];
		}
		
		//populate the array as required
		for (int i = 0; i < second; i++) {
			for (int j = 0; j < first; j++) {
				if (check.get(i).equals(MovieQuery.TITLE)) {
					returnArray[j][i] = filtered.get(j).getTitle();
				}
				else if (check.get(i).equals(MovieQuery.AWARDS)) {
					returnArray[j][i] = Integer.toString(filtered.get(j).getAwards());
				}
				else if (check.get(i).equals(MovieQuery.VOTES)) {
					returnArray[j][i] = Integer.toString(filtered.get(j).getVotes());
				}
			}
		}
		
		return returnArray;
	}
}