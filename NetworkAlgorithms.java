package edu.upenn.cis.cis121.project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 
 * This class contains algorithms that process information obtained through
 * the DBWrapper class. 
 * 
 * @author Jevon Yeoh (jevony@seas.upenn.edu)
 * CIS 121 - Final Project - Spring 2012
 * 
 */

public class NetworkAlgorithms {

	private String _dbUser;
	private String _dbPass;
	private String _dbSID;
	private String _dbHost;
	private int _port;
	private HashSet<Integer> _allUsers;
	private HashSet<Integer> _allPlaces;
	private HashMap<Integer, int[]> _userToPlace;
	private HashMap<Integer, String> _placeToType;

	
	/**
	 * 
	 * Constructs a NetworkAlgorithms object and populates relevant 
	 * datastructures to allow for a reduced amount of time to calculate 
	 * required information by reducing the number of queries required to
	 * the database.
	 * 
	 * @param dbUser
	 * @param dbPass
	 * @param dbSID
	 * @param dbHost
	 * @param port
	 */
	public NetworkAlgorithms(String dbUser, String dbPass, String dbSID, String dbHost, int port) {	
		_dbUser = dbUser;
		_dbPass = dbPass;
		_dbSID = dbSID;
		_dbHost = dbHost;
		_port = port;
		
		DBWrapper access = new DBWrapper(_dbUser, _dbPass, _dbSID, _dbHost, _port);
		_allUsers = access.getAllUsers();
		_userToPlace = new HashMap<Integer, int[]>();
		_placeToType = new HashMap<Integer, String>();
		
		Iterator<Integer> iterator = _allUsers.iterator();
		while (iterator.hasNext()) {
			
			//populate userToPlace hashmap
			int temp = iterator.next();
			int[] placesLiked = access.getLikes(temp);
			_userToPlace.put(temp, placesLiked);

			
			//populate placeToType hashmap
			for (int i = 0; i < placesLiked.length; i++) {
				int currPlaceID = placesLiked[i];
				if (!_placeToType.containsKey(currPlaceID)) {
					String desc = access.getDescription(currPlaceID);
					_placeToType.put(currPlaceID, desc);
				}
			}		
		}
	}
		
	/**
	 * 
	 * Calculates the bacon number between two individuals. The bacon number is
	 * the minimum number of friendships between two people.
	 * 
	 * @param user_id1 - ID of first person
	 * @param user_id2 - ID of second person
	 * @return the bacon number of the two people. If user_id2 cannot be reached
	 * from user_id1, returns -1.
	 * @throws IllegalArgumentException when user_id does not exist in database
	 */
	public int distance(int user_id1, int user_id2) throws IllegalArgumentException {
		
		//edge case handling
		if (user_id1 == user_id2) {
			return 0;
		}
		
		//edge case handling
		if (!_allUsers.contains(user_id1) || !_allUsers.contains(user_id2)) {
			throw new IllegalArgumentException();
		}
		
		//create relevant datastructures
		DBWrapper access = new DBWrapper(_dbUser, _dbPass, _dbSID, _dbHost, _port);
		LinkedList<Integer> queue = new LinkedList<Integer>();
		HashSet<Integer> seen = new HashSet<Integer>();
		HashMap<Integer, Integer> baconMap = new HashMap<Integer, Integer>();

		//update datastructures with point person
		seen.add(user_id1);
		baconMap.put(user_id1, 0);
		queue.add(user_id1);
		
		//run BFS to find bacon number
		while (!queue.isEmpty()) {
			int node = queue.remove();
			int[] checkFriends = access.getFriends(node);
			
			for (int compare : checkFriends) {
				if (compare == user_id2) {
					return baconMap.get(node) + 1;
				}
				
				if (!seen.contains(compare)) {
					seen.add(compare);
					baconMap.put(compare, baconMap.get(node) + 1);
					queue.add(compare);
				}
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * This algorithm recommends friends to a current user based on the formula
	 * weight(user_id1, user_id2) = 1/[(num places in common) + 0.1 * 
	 * (num place_types in common) + 0.01]. A smaller edge weight represents
	 * a better recommendation.
	 * 
	 * @param user_id - ID of person to find friend recommendations for
	 * @param numRec - number of people to recommend
	 * @return a list of user_ids to recommend 
	 * @throws IllegalArgumentException when numRec < 1 and when user_id is not
	 * in the database
	 */
	public List<Integer> recommendFriends(int user_id, int numRec) 
			throws IllegalArgumentException {
		
		if (numRec < 1) {
			throw new IllegalArgumentException();
		}
		
		if (!_allUsers.contains(user_id)) {
			throw new IllegalArgumentException();
		}
		
		DBWrapper access = new DBWrapper(_dbUser, _dbPass, _dbSID, _dbHost, _port);
		List<Integer> output = new ArrayList<Integer>();
		
		//create relevant data-structures 
		HashMap<Integer, Double> vertexWeight = new HashMap<Integer, Double>(); //map user_id to weight
		PriorityQueue<Vertex> heap = new PriorityQueue<Vertex>(); //monitor which friends to pop
		HashSet<Integer> explored = new HashSet<Integer>(); //list of explored people
		HashSet<Integer> userFriends = new HashSet<Integer>();
		HashSet<Integer> recommendedFriends = new HashSet<Integer>();
		
		//create vertex for the point user
		int[] friends = access.getFriends(user_id);
		explored.add(user_id);
		heap.add(new Vertex(user_id, 0)); 
		userFriends.add(user_id); 
		vertexWeight.put(user_id, 0.0); 
		
		//find weights for first-degree friends
		for (int i = 0; i < friends.length; i++) {
			double tempWeight = getWeight(user_id, friends[i], access);
			vertexWeight.put(friends[i], tempWeight);
			Vertex person = new Vertex(friends[i], tempWeight);
			heap.add(person);
			explored.add(friends[i]);
			userFriends.add(friends[i]);
		}
		
		while (output.size() < numRec && !heap.isEmpty() ) {
			Vertex current = heap.poll();
			
			// only people who are not the user or not friends of the user will be added
			if (!userFriends.contains(current.getID())
					&& !recommendedFriends.contains(current.getID())) {
				output.add(current.getID());
				recommendedFriends.add(current.getID());
			}
			
			if (output.size() == numRec) {
				break;
			}
			
			int[] currentFriends = access.getFriends(current.getID());

			for (int temp : currentFriends) {
				double weight = getWeight(current.getID(), temp, access);
				
				//add weight into relevant data structures
				if (!explored.contains(temp)) {
					explored.add(temp);
					vertexWeight.put(temp, weight + current.getWeight());
					heap.add(new Vertex(temp, weight + current.getWeight()));
				}

				//update if weight of node is smaller, otherwise don't do anything
				else if (weight + current.getWeight() < vertexWeight.get(temp)) {
					//update hashmap of weights
					vertexWeight.remove(temp);
					vertexWeight.put(temp, weight + current.getWeight());
					
					//update PQ of vertices
					heap.remove(new Vertex(temp, vertexWeight.get(temp)));
					heap.add(new Vertex(temp, weight + current.getWeight()));	
				}
			}
		}	
		return output;
	}
	
	/**
	 * Helper method to calculate the weight between two people.
	 * 
	 * @param user_id1 - ID of first person
	 * @param user_id2 - ID of second person
	 * @param access DBwrapper object to use accessor methods
	 * @return the weight between 2 people
	 */
	private double getWeight(int user_id1, int user_id2, DBWrapper access) {
		
		int[] user1places = _userToPlace.get(user_id1);
		HashMap<String, Integer> placeTypes1 = new HashMap<String, Integer>();
		//placeTypesAll contains all the descriptions that both user1 and user2
		//like
		HashSet<String> placeTypesAll = new HashSet<String>();
		
		//obtain description of places that user1 likes and count the number
		//of times the person likes each description
		for (int i = 0; i < user1places.length; i++) {
			String description = _placeToType.get(user1places[i]);			
			placeTypesAll.add(description);
			if (!placeTypes1.containsKey(description)) {
				placeTypes1.put(description, 1);
			}
			else {
				int temp = placeTypes1.get(description);
				temp++;
				placeTypes1.put(description, temp);
			}
		}
		
		int[] user2places = _userToPlace.get(user_id2);
		int samePlaces = 0;
		
		//count number of places that both user1 and user2 like
		for (int i = 0; i < user1places.length; i++) {
			for (int j = 0; j < user2places.length; j++) {
				if (user1places[i] == user2places[j]) {
					samePlaces++;
				}
			}
		}
		
		HashMap<String, Integer> placeTypes2 = new HashMap<String, Integer>();
		
		//obtain description of places that user2 likes and count the number
		//of times the person likes each description
		for (int i = 0; i < user2places.length; i++) {
			String description = _placeToType.get(user2places[i]);
			placeTypesAll.add(description);
			if (!placeTypes2.containsKey(description)) {
				placeTypes2.put(description, 1);
			}
			else {
				int temp = placeTypes2.get(description);
				temp++;
				placeTypes2.put(description, temp);
			}
		}
		
		//traverse through all the descriptions and if both users liked such a 
		//place, add the minimum to obtain the place types in common
		int samePlaceTypes = 0;
		Iterator<String> iterator = placeTypesAll.iterator();
		while (iterator.hasNext()) {
			String desc = iterator.next();
			if (placeTypes1.containsKey(desc) && placeTypes2.containsKey(desc)) {
				int first = placeTypes1.get(desc);
				int second = placeTypes2.get(desc);
				samePlaceTypes += Math.min(first, second);
			}
		}

		//calculate weight based on formula
		double weight = 1 / (samePlaces + (0.1*samePlaceTypes) + 0.01);

		return weight;
	}
	
	/**
	 * This inner class provides a representation of people. It implements
	 * Comparable such that it can be used in the PriorityQueue.
	 *
	 */
	private class Vertex implements Comparable<Vertex> {
		
		private int _user_id;
		private double _weight;
		
		//construct new "person"
		public Vertex(int user_id, double weight) {
			_user_id = user_id;
			_weight = weight;
		}
		
		//a vertex with a smaller weight is deemed to be smaller
		public int compareTo(Vertex other) {
			if (_weight < other.getWeight()) {
				return -1;
			}
			else if (_weight == other.getWeight()) {
				return 0;
			}
			else {
				return 1;
			}
		}
		
		//accessor method to return user_id
		public int getID() {
			return _user_id;
		}
		
		//accessor method to return weight of user
		public double getWeight() {
			return _weight;
		}
	}
	
	/**
	 * 
	 * This method takes as input a user and a few constraints, and returns a
	 * formatted set of friends and suggested places to go with those friends.
	 * It also has information about the user with user_id. 
	 * 
	 * This algorithm begins by finding the maxFriends geographically closest 
	 * friends to the user with user_id. Since user_id may have fewer than 
	 * maxFriends friends, it is just the maximum number of friends that may 
	 * be returned. Using this list of friends, we find the most suitable places 
	 * to visit using the suitability formula as follows:
	 * 
	 * suitability(p) = (number of likes for p among close friends) /
	 * [(distance from center of friends to p) + 0.01]
	 * 
	 * The larger the p, the more suitable.
	 * @param user_id - ID of person 
	 * @param maxFriends - maximum number of people to recommend
	 * @param maxPlaces - maximum number of places to recommend
	 * @return formatted string of friends and suggested places to go with those
	 * friends
	 * @throws IllegalArgumentException when maxFriends and maxPlaces are negative
	 * and when user_id does not exist in database
	 */
	public String recommendActivities(int user_id, int maxFriends, int maxPlaces) 
			throws IllegalArgumentException {
		
		//handle edge cases
		if (maxFriends == 0) {
			return "";
		}
		
		if (maxPlaces == 0) {
			return "";
		}
		
		if (maxFriends < 0 || maxPlaces < 0) {
			throw new IllegalArgumentException();
		}
		
		if (!_allUsers.contains(user_id)) {
			throw new IllegalArgumentException();
		}
		
		DBWrapper access = new DBWrapper(_dbUser, _dbPass, _dbSID, _dbHost, _port);
		
		//get all friends
		int[] friends = access.getFriends(user_id);
		double[] userLoc = access.getUserLocation(user_id);
		int[] closeFriends;
		PriorityQueue<Friend> distances = new PriorityQueue<Friend>(); //friends sorted according to distance
		HashMap<Integer, Integer> places = new HashMap<Integer, Integer>(); //map place_id to number of likes
		PriorityQueue<Place> sortedPlaces = new PriorityQueue<Place>(); //sorted according to suitability
		
		//create close friends array
		if (friends.length < maxFriends) {
			closeFriends = new int[friends.length];
		}
		else {
			closeFriends = new int[maxFriends];
		}
		
		//get location of friends
		for (int a : friends) {
			double[] friendLoc = access.getUserLocation(a);
			double distance = friendDistance(userLoc, friendLoc);
			Friend temp = new Friend(a, distance);
			distances.add(temp);
		}

		//find closest friends
		for (int i = 0; i < closeFriends.length; i++) {
			int save = distances.poll().getFriendUser();
			closeFriends[i] = save;
		}
		
		//find center of friends
		double[] centerOfFriends = new double[2]; //centeroffriends location
		double totalLat = 0;
		double totalLong = 0;
		for (int i = 0; i < closeFriends.length; i++) {
			double[] tempLoc = access.getUserLocation(closeFriends[i]);
			totalLat += tempLoc[0];
			totalLong += tempLoc[1];
		}
		centerOfFriends[0] = totalLat / closeFriends.length;
		centerOfFriends[1] = totalLong / closeFriends.length;

		
		//find places and number of likes
			//for self
		int[] myLikedPlaces = access.getLikes(user_id);
		for (int i = 0; i < myLikedPlaces.length; i++) {
			int temp = myLikedPlaces[i];
			if (!places.containsKey(temp)) {
				places.put(temp, 1);
			}
			else {
				int temp2 = places.get(temp);
				temp2++;
				places.put(temp, temp2);
			}
		}
			//for friends
		for (int i = 0; i < closeFriends.length; i++) {
			int[] friendsLikedPlaces = access.getLikes(closeFriends[i]);
			for (int j = 0; j < friendsLikedPlaces.length; j++) {
				int temp = friendsLikedPlaces[j];
				if (!places.containsKey(temp)) {
					places.put(temp, 1);
				}
				else {
					int temp2 = places.get(temp);
					temp2++;
					places.put(temp, temp2);
				}
			}
		}
		
		//find suitability
		Set<Integer> allPlaces = places.keySet();
		Iterator<Integer> iterator = allPlaces.iterator();
		while (iterator.hasNext()) {
			int key = iterator.next();
			int numLikes = places.get(key);
			double suit = (numLikes / (friendDistance(access.getLocation(key), centerOfFriends))) + 0.01;
			Place create = new Place(key, suit);
			sortedPlaces.add(create);
		}
		
		int[] finalPlaces = new int[maxPlaces];
		for (int i = 0; i < maxPlaces; i++) {
			if (!sortedPlaces.isEmpty()) {
				Place temp = sortedPlaces.poll();
				finalPlaces[i] = temp.getPlaceID();
			}
		}

		//create JSON output string
		String JSONoutput = "";
		String userOut = "{\n  \"user\":" + userToString(user_id, access) + "  },\n";
		
		String friendOut = "  \"friends\": {\n";
		for (int i = 0; i < closeFriends.length; i++) {
			friendOut += "    \"" + i + "\" :";
			friendOut += userToString(closeFriends[i], access); 
			friendOut += "},";
		}
		friendOut += "  },\n";
			
		String placeOut = "  \"places\": {\n";
		for (int i = 0; i < finalPlaces.length; i++) {
			placeOut += "    \"" + i + "\" :";
			placeOut += placeToString(finalPlaces[i], access);
			placeOut += "},";
		}
		placeOut += "  }";
		
		JSONoutput = userOut + friendOut + placeOut + "\n}";
	
		return JSONoutput;
	}
	
	/**
	 * 
	 * Helper method to convert information about a place into the appropriately
	 * formatted string.
	 * 
	 * @param place_id - ID of place
	 * @param access - DBWrapper object
	 * @return appropriately formatted string
	 */
	private String placeToString(int place_id, DBWrapper access) {
		String[] input = access.getPlaceData(place_id);
		return " {\"place_id\":" + place_id + ",\n"
				+ "\t   \"place_name\":\"" + input[0] + "\",\n"
				+ "\t   \"description_name\":\"" + input[1] + "\",\n"
				+ "\t   \"latitude\":" + input[2] + ",\n"
				+ "\t   \"longitude\":" + input[3]+ "\n";
	}
	
	/**
	 * 
	 * Helper method to convert information about a user into the appropriately
	 * formatted string.
	 * 
	 * @param user_id - ID of user
	 * @param access - DBWrapper object
	 * @return appropriately formatted string
	 */
	private String userToString(int user_id, DBWrapper access) {
		String[] input = access.getUserData(user_id);
		return " {\"user_id\":" + user_id + ",\n"
				+ "\t   \"first_name\":\"" + input[0] + "\",\n"
				+ "\t   \"last_name\":\"" + input[1] + "\",\n"
				+ "\t   \"latitude\":" + input[2] + ",\n"
				+ "\t   \"longitude\":" + input[3]+ "\n";
	}
	
	/**
	 * 
	 * Helper method to calculate geographical distance between 2 people.
	 * @param userLoc - location of first person
	 * @param friendLoc - location of second person
	 * @return distance between the two people
	 */
	private double friendDistance(double[] userLoc, double[] friendLoc) {
		double lat1 = userLoc[0];
		double long1 = userLoc[1];
		double lat2 = friendLoc[0];
		double long2 = friendLoc[1];
		return Math.sqrt(Math.pow((lat1 - lat2), 2) + Math.pow((long1 - long2), 2));
	}
	
	/**
	 * 
	 * This inner class stores information about a particular place and 
	 * its suitability between people. It implements Comparable such that it 
	 * can be used in the PriorityQueue.
	 *
	 */
	private class Place implements Comparable<Place> {

		private double _suitability;
		private int _place_id;		
		
		public Place(int place_id, double suitability) {
			_place_id = place_id;
			_suitability = suitability;
		}
		
		//accessor method to get suitability
		public double getSuitability() {
			return _suitability;
		}
		
		//accessor method to get place ID
		public int getPlaceID() {
			return _place_id;
		}
		
		//greater suitability means smaller place when being compared on a 
		//min heap
		public int compareTo(Place o) {
			if (_suitability > o.getSuitability()) {
				return -1;
			}
			else if (_suitability == o.getSuitability()) {
				return 0;
			}
			else {
				return 1;
			}
		}
		
	}
	
	/**
	 * 
	 * This inner class stores information about a particular friend and 
	 * his distance between people. It implements Comparable such that it 
	 * can be used in the PriorityQueue.
	 *
	 */
	private class Friend implements Comparable<Friend> {
		
		private int _user_id;
		private double _distance;
		
		public Friend(int id, double dist) {
			_user_id = id;
			_distance = dist;
		}
		
		//accessor method to get user_id
		public int getFriendUser() {
			return _user_id;
		}
		
		//accessor method to get distance
		public double getDistance() {
			return _distance;
		}

		//smaller distance means smaller friend
		public int compareTo(Friend other) {
			if (_distance < other.getDistance()) {
				return -1;
			}
			else if (_distance == other.getDistance()) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	
	/**
	 * 
	 * This method closes the database connection.
	 * 
	 */
	public void closeDBConnection(){
		try {
			DBUtils.closeDBConnection();
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * This method recommends numRec number of places to user_id to go to.
	 * It does this by first recommending the places which the users 
	 * immediate friends recommends the most. If there are still insufficient
	 * recommendations, this method then recommends places that are the closest
	 * to the user.
	 * 
	 * @param user_id - ID of person to find places to recommend for
	 * @param numRec - number of places to recommend
	 * @return - list of place_id's of recommendations
	 * @throws IllegalArgumentException when user_id is not found in the 
	 * database and when numRec < 1
	 */
	public List<Integer> recommendPlaces(int user_id, int numRec) 
					throws IllegalArgumentException {
		
		if (!_allUsers.contains(user_id)) {
			throw new IllegalArgumentException();
		}
		
		if (numRec < 1) {
			throw new IllegalArgumentException();
		}
		
		//create relevant data-structures and objects
		DBWrapper access = new DBWrapper(_dbUser, _dbPass, _dbSID, _dbHost, _port); 
		HashSet<Integer> userLikes = new HashSet<Integer>();
		HashSet<Integer> possible = new HashSet<Integer>();
		HashMap<Integer, Integer> placeToLikes = new HashMap<Integer, Integer>();
		PriorityQueue<Place> sortedPlaces = new PriorityQueue<Place>();
		List<Integer> output = new ArrayList<Integer>();
		
		
		//get list of places the user like
		int[] placesUserLikes = _userToPlace.get(user_id);
		for (int i = 0; i < placesUserLikes.length; i++) {
			userLikes.add(placesUserLikes[i]);
		}
		
		//get first degree friends
		int[] friends = access.getFriends(user_id);
		
		//compare places
		for (int i = 0; i < friends.length; i++) {
			int[] placesFriendLikes = _userToPlace.get(friends[i]);
			//if user doesn't like a place friend likes, add to list of possible
			//places to recommend
			for (int j = 0; j < placesFriendLikes.length; j++) {
				if (!userLikes.contains(placesFriendLikes[j])) {
					possible.add(placesFriendLikes[j]);
				}
				
				//update map with number of likes a certain place has based on 
				//friends
				if (!placeToLikes.containsKey(placesFriendLikes[j])) {
					placeToLikes.put(placesFriendLikes[j], 1);
				}
				else {
					int temp = placeToLikes.get(placesFriendLikes[j]);
					temp++;
					placeToLikes.put(placesFriendLikes[j], temp);
				}
			}
		}
	
		//create sorted queue to order places
		Iterator<Integer> it = possible.iterator();
		while (it.hasNext()) {
			int place = it.next();
			Place temp = new Place(place, placeToLikes.get(place));
			sortedPlaces.add(temp);			
		}
		
		//add to output if relevant
		while (output.size() < numRec && !sortedPlaces.isEmpty()) {
			Place curr = sortedPlaces.poll();
			output.add(curr.getPlaceID());
		}
		
		//if there still isn't enough places to go to,
		//get all the places he and his friends haven't been to
		//recommend the closest place to him (lazy person haha)
		if (output.size() < numRec) {
			_allPlaces = access.getAllPlaces();
			PriorityQueue<Double> remainder = new PriorityQueue<Double>();
			HashMap<Double, Integer> distanceToPlace = new HashMap<Double, Integer>();
			double[] userLoc = access.getUserLocation(user_id);
			
			Iterator<Integer> it3 = _allPlaces.iterator();
			while (it3.hasNext()) {
				int currPlace = it3.next();
				if (!userLikes.contains(currPlace) && !possible.contains(currPlace)) {
					double[] placeLoc = access.getLocation(currPlace);
					double distance = friendDistance(placeLoc, userLoc); 
					remainder.add(distance);
					distanceToPlace.put(distance, currPlace);
				}
			}
			
			while (output.size() < numRec) {
				double rec = remainder.poll();
				output.add(distanceToPlace.get(rec));
			}
		}
		
		return output;
	}

}
