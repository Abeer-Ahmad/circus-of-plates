package mvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

import collections.Shapes;
import plateGenerator.Belt;
import plateGenerator.LeftBelt;
import plateGenerator.RightBelt;
import shapes.Shape;
import shapes.ShapeStates;
import system.Player;

public class Model extends Observable {

	private ArrayList<Belt> belts;
	private ArrayList<Player> players;
	private ArrayList<Shape> shapes;
	private long shapeTimer;
	private boolean isRunning;
	private String level;
	private String firstPlayerTool;
	private ScoreManager scoreManager;
	private int laserHeight;
	
	public Model(Observer gameViewer) {
		players = new ArrayList<Player>();
		shapes = new ArrayList<Shape>();
		belts = new ArrayList<Belt>();
		shapeTimer = 0;
		isRunning = false;
		addObserver(gameViewer);
		scoreManager = null;
		laserHeight = 140;
	}

	private void setBelts(int x) {
		belts.add(new LeftBelt(0, 50, 400));
		belts.add(new LeftBelt(0, 100, 250));
		belts.add(new RightBelt(x, 50, 400));
		belts.add(new RightBelt(x, 100, 250));
	}

	public void setPlayers(boolean twoPlayers, ArrayList<String> names,int xFrame, int yFrame) {

		players.add(new Player(0,yFrame, names.get(0)));
		if (twoPlayers) {
			players.add(new Player(xFrame-200,yFrame, names.get(1)));
		}
	}

	public void setLevel(String dataLevel) {
		this.level = dataLevel;
		for (Belt belt : belts) {
			belt.getRandomGenerator().setDifficultyLevel(dataLevel);
		}
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

	public String getFirstPlayerTool() {
		return firstPlayerTool;
	}

	public void movePlayer(Player player, int step) {
		
		player.move(step);
		setChanged();
		notifyObservers(players);
		player.manageStack(shapes);
		setChanged();
		notifyObservers(shapes);

	}

	private void addNewShape() {
		if (shapeTimer % 20 == 0) {
			for (Belt belt : belts) {
				shapes.add(belt.addShape());
			}
		}
		shapeTimer++;
		if (shapeTimer > 1000000000) {
			shapeTimer = 1;
		}
	}

	private void removeExpired() {

		int size = shapes.size();
		for (int i = size - 1; i >= 0; --i) {
			Shape shape = shapes.get(i);
			ShapeStates state = shape.getShapeState();
			if ((state == ShapeStates.onGround) || (state == ShapeStates.captured)) {
				shapes.remove(i);
			}
		}
		setChanged();
		notifyObservers(shapes);

	}

	private void updateShapes() {
		for (Shape shape : shapes) {
			shape.update();
		}
	}

	private void updatePlayers() {
		for (Player player : players) {
			player.manageStack(shapes);
		}
		setChanged();
		notifyObservers(players);
	}

	public synchronized void startGame(LinkedHashMap<String, Object> settings) {
		restart();
		boolean twoPlayers = (boolean) settings.get("twoPlayers");
		ArrayList<String> names = (ArrayList<String>) settings.get("names");
		int xFrame= (int) settings.get("dimX");
		int yFrame = (int) settings.get("dimY");
		firstPlayerTool = (String) settings.get("tool");
		setPlayers(twoPlayers, names,xFrame,yFrame);
		setChanged();
		Boolean twoBPlayers = new Boolean(twoPlayers);
		notifyObservers(twoBPlayers);
		/* after game grid is intialized*/
		setBelts(xFrame);
		setChanged();
		notifyObservers(belts);
		/* should be called after belts set*/
		setLevel((String) settings.get("level"));
		scoreManager =ScoreManager.getInstance(players, yFrame- laserHeight);
		updateGameItems();
		isRunning = true;
		notify();
		
	}

	private void restart() {
		shapes.clear();
		players.clear();
	}

	public void pauseGame() {
		isRunning = false;
	}

	public synchronized void newLevel(){
		shapes.clear();
		for (Player player : players) {
			player.newLevel();
		}
		isRunning=true;
		notify();
	}
	public synchronized void continueGame() {
		isRunning = true;
		notify();
	}

	public void updateGameItems() {
		addNewShape();
		updateShapes();
		updatePlayers();
		removeExpired();
		if (scoreManager.isOver()) {
			System.out.println("game over");
			isRunning = false;
			setChanged();
			notifyObservers(scoreManager.getWinner());
		}
	}

	public synchronized boolean isRunning() throws InterruptedException {
		if (!isRunning) {
			wait();
		}
		return isRunning;
	}
}
