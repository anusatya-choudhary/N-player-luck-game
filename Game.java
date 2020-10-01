package Game;

import java.util.*;

class SharedData {
	public int numberOfPlayers;
	public boolean isGameComplete;
	public boolean[] playerChance;
	public boolean[] hasPlayerWon;
	public boolean play = false;
	public int currentNumber;// the only thing that is visible to the user.
	public Object lock;
	
	public SharedData(int n) 
	{
		this.numberOfPlayers = n;
		this.isGameComplete = false;
		this.lock = new Object();
		this.playerChance = new boolean[this.numberOfPlayers];
		this.hasPlayerWon = new boolean[this.numberOfPlayers];
	}
}

/* Moderator Class */
class Moderator implements Runnable {
	private SharedData sharedData; 
	private final int turns = 10;// total 10 turns as specified in the question
	private ArrayList<Integer> numbersAnnounced;
	private final int timeToWait = 60; //in seconds[Problem statement asks for 1 minute wait]
	private int turnsPlayed;

	public Moderator(SharedData data) 
	{
		numbersAnnounced = new ArrayList<Integer>();
		this.sharedData = data;
		this.turnsPlayed = 0;
	}

	//Random Number generator
	private int generateNumbers(int lo, int hi) 
	{
		Random rand = new Random();
		return ((rand.nextInt(hi - lo + 1)) + lo);
	}

	
	// multi - threading logic for moderator
	public void run() 
	{
		synchronized (sharedData.lock) 
		{
			while (true) 
			{
				boolean endGame = false;
				for (int i = 0; i < sharedData.numberOfPlayers; i++) 
				{
					if (sharedData.hasPlayerWon[i]) 
					{
						endGame = true;
						break;
					}
				}
				if (endGame)
					break;

				/* moderator starts the game; initialization*/
				for (int i = 0; i < sharedData.numberOfPlayers; i++) 
				{
					sharedData.playerChance[i] = false;
				}
				sharedData.play = false;//round is not played yet
				sharedData.currentNumber = this.generateNumbers(0, 50);// the only number that is visible to the user
				(this.numbersAnnounced).add(sharedData.currentNumber);
				System.out.println("The number announced is " + sharedData.currentNumber);// prints the current announced number
				sharedData.play = true;
				sharedData.lock.notifyAll();// inform all the player threads that the number has been announced

				/* Time for the moderator thread to go to sleep
				 * The moment the number is announced and all other threads have been notified
				*/
				
				if (!sharedData.isGameComplete) 
				{
					try 
					{
						Thread.sleep(1000 * this.timeToWait); //send this thread to sleep.
					} 
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				boolean haveChecked = false;
				// wait until all the players have played a round
				while (true) 
				{
					haveChecked = true;
					// check if all players have played
					for (int i = 0; i < sharedData.numberOfPlayers; i++)
					{
						if (!sharedData.playerChance[i])
						{
							haveChecked = false;
							break;
						}
					}
					if (haveChecked) 
					{
						break;
					}
					// wait for a notification
					try 
					{
						sharedData.lock.wait();
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}

				this.turnsPlayed++;
				System.out.println("Turns played: " + this.turnsPlayed);
				if (this.turnsPlayed >= this.turns) 
				{
					// game over
					sharedData.isGameComplete = true;
				}
			}

			// all players have played one round
			for (int i = 0; i < sharedData.numberOfPlayers; i++) 
			{
				if (sharedData.hasPlayerWon[i]) 
				{
					System.out.println("Player " + (i + 1) + " has won the game");
				}
			}
			sharedData.isGameComplete = true;// turn completed

			if (sharedData.isGameComplete)
				System.exit(0);//exit if the game is completed
			sharedData.lock.notifyAll();
		}
	}
}

/*
 * Carries the architecture for the player.
 */
class Player implements Runnable {
	private SharedData sharedData;
	private int[] card;// stores the random values on a card
	private int strikes;
	public int playerID; // uniquely identifies every player. 
	public final int LIM = 10;// number of numbers each card carries as defined in question
	private final int totalStrikes = 3;// termination condition
	private boolean[] flag;

	private int generateNumber(int lo, int hi) 
	{ // random number generator for the players
		Random rand = new Random();
		return rand.nextInt(hi - lo + 1) + lo;
	}

	// Initialization of the player
	public Player(int id, SharedData data) 
	{
		this.sharedData = data;
		this.playerID = id;// assign player ID

		// generate numbers
		this.card = new int[LIM];
		this.flag = new boolean[LIM];
		for (int i = 0; i < LIM; i++)
		{
			this.card[i] = generateNumber(0, 50);//generates a random number between 0 and 50
			this.flag[i] = false;
		}
		this.strikes = 0;//initially no number has been struck
	}

	// Termination Logic
	private boolean checkTermination()
	{
		return (this.strikes >= this.totalStrikes);
	}

	// multi-threading logic
	public void run() 
	{
		synchronized (sharedData.lock)
		{
			while (!sharedData.isGameComplete)
			{
				/*
				 * If it's not the player's chance you need to wait or the number has not been
				 * announced yet
				 */
				while ((!sharedData.play) || (sharedData.playerChance[this.playerID])) 
				{
					try 
					{
						sharedData.lock.wait();
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}

				if (!sharedData.isGameComplete) 
				{
					for (int i = 0; i < this.LIM; i++) 
					{
						if (sharedData.currentNumber == this.card[i] && flag[i] == false) 
						{ // card found
							flag[i] = true;
							strikes++;
						}
					}
					if (this.checkTermination() == true) 
					{
						sharedData.hasPlayerWon[this.playerID] = true;
					}
					sharedData.playerChance[this.playerID] = true;
					sharedData.lock.notifyAll();
				}
			}
			if (sharedData.isGameComplete)
				System.exit(0);//end running the code if the game is played
		}
	}
}

public class Game {

	public static void main(String[] args) 
	{
		System.out.println("Enter the number of Players who will play the game");
		
			int n = read.nextInt();
			// initialize players
			SharedData gameDetails = new SharedData(n);
			// Start the Game
			Moderator moderator = new Moderator(gameDetails);
			Thread moderatorThread = new Thread(moderator);
			Player[] player = new Player[gameDetails.numberOfPlayers];
			Thread[] playerThread = new Thread[gameDetails.numberOfPlayers];
			for (int i = 0; i < gameDetails.numberOfPlayers; i++)
			{
				player[i] = new Player(i, gameDetails);// constructor needs to be defined
				playerThread[i] = new Thread(player[i]);
			}
			// start the threads
			moderatorThread.start();
			for (int i = 0; i < gameDetails.numberOfPlayers; i++) 
			{
				playerThread[i].start();
			}
		
	}
}
/*
 * Strategy and Stages 1. Moderator displays the number 2. Every one of the
 * threads can now access this number and cross it off if there is a match. 
 * 3. Run a checker to see if the conditions for termination are met.
 * Termination Constraints: 1. Some player wins the game or 10 turns have been played already
 */
