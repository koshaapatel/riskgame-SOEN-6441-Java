package com.riskgame.controller;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.riskgame.config.StageManager;
import com.riskgame.model.GamePlayPhase;
import com.riskgame.model.Player;
import com.riskgame.model.PlayerTerritory;
import com.riskgame.model.RiskMap;
import com.riskgame.observerpattern.Observer;
import com.riskgame.service.Impl.MapManagementImpl;
import com.riskgame.service.Impl.RiskPlayImpl;
import com.riskgame.view.FxmlView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * This a game playing screen controller, where things like Reinforcement,
 * attack and fortification managed and player can play the game through command
 * line also. This screen got redirected from startup phase controller.
 * 
 * @author <a href="mailto:p_thumma@encs.concordia.ca">Piyush Thummar</a>
 * @see com.riskgame.controller.StartupPhaseController
 */
@Controller
public class RiskPlayScreenController extends Observer implements Initializable {

	GamePlayPhase gameplayphase = new GamePlayPhase();

	@FXML
	private Button btnFireCommand;

	@FXML
	private TextArea txtPhaseView;

	@FXML
	private TextArea txtPlayerDominationView;

	@FXML
	private TextArea txtCardExchangeView;

	@FXML
	private TextField txtCommandLine;

	@FXML
	private TextArea txtConsoleLog;

	@FXML
	private Button btnExit;

	@FXML
	private TextArea territoryArea;

	@FXML
	private TextArea adjacentTerritoryArea;

	@FXML
	private TextArea currentLog;

	@FXML
	private TextArea phaseviewLog;

	@Autowired
	public MapManagementImpl mapManagementImpl;

	@Autowired
	public RiskPlayImpl riskPlayImpl;

	@Lazy
	@Autowired
	private StageManager stageManager;

	private GamePlayPhase gamePlayPhase;

	private RiskMap riskMap;

	private StringBuilder sb;

	private int playerIndex = 0;
	private String playerName = "";
	// private int playerLeftArmy = 0;
	private int playerReinforceArmy = 0;

	private static String turnStartedMsg = "";
	private static String leftArmyMsg = "";

	public static final String NEUTRAL = "NEUTRAL";

	private static String NEWLINE = System.getProperty("line.separator");

	private static boolean mapConquered = false;
	private static boolean countryConqured = false;
	private static boolean attackphaseEnded = false;
	private static int attackerDice;
	private static int defenderDice;

	private Player defenderPlayer;
	private String fromCountryAttack;
	private String toCountryAttack;

	private boolean attackFire = false;

	private ObservableList<Player> playerList = FXCollections.observableArrayList();

	/**
	 * This is an initialization method for this controller to start.
	 * 
	 * @param location
	 *            of the FXML file
	 * @param resources
	 *            is properties information
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 *      java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		gamePlayPhase = new GamePlayPhase();
		riskMap = new RiskMap();
		sb = new StringBuilder();

		territoryArea.clear();
		adjacentTerritoryArea.clear();
	}

	/**
	 * This is an onAction method for button fireCommand. It'll take commands from
	 * player and performs action accordingly.
	 * 
	 * @param event
	 *            will represents value sent from view
	 */
	@FXML
	void fireCommand(ActionEvent event) {
		// txtConsoleLog.clear();
		fillTerritoryList();
		fillAdjacentTerritoryList();
		System.out.println("playerIndex => " + playerIndex);
		System.out.println("playerName => " + playerName);
		System.out.println("playerReinforceArmy => " + playerReinforceArmy);

		System.out.println("List = > " + playerList);

		try {
			String command = txtCommandLine.getText();
			if (command != null && !command.isEmpty()) {
				if (command.startsWith("reinforce") || command.startsWith("exchangecards")) {
					if (playerReinforceArmy != 0) {
						if (countryConqured) {

						}

						txtConsoleLog.setText(placeReinforcement(command));
					} else {
						sb.append(playerName).append(" 's Reinforcement Phase done please go to the Attack phase")
								.append(NEWLINE);
						txtConsoleLog.setText(sb.toString());
					}

				} else if (command.startsWith("attack") || command.startsWith("defend")
						|| command.startsWith("attackmove")) {

					if (playerReinforceArmy == 0) {

						txtConsoleLog.setText(attackPhase(command));

					} else {
						sb.append("you have left ").append(playerReinforceArmy).append(" to reinforcement")
								.append(NEWLINE);
						txtConsoleLog.setText(sb.toString());
					}

				} else if (command.startsWith("fortify")) {
					if (playerReinforceArmy == 0) {
						if (attackphaseEnded) {
							txtConsoleLog.setText(fortification(command));
						} else {
							sb.append("Please fire \"attack -noattck\" before starting fortification phase");
						}
					} else {
						sb.append("you have left ").append(playerReinforceArmy).append(" to reinforcement")
								.append(NEWLINE);
						txtConsoleLog.setText(sb.toString());
					}
				} else
					txtConsoleLog.setText("Please Enter Valid Command");
			} else {
				txtConsoleLog.setText("Please Enter valid Command");
			}

		} catch (Exception e) {
			e.printStackTrace();
			txtConsoleLog.setText("Please Enter Valid Command");
		}
	}

	/**
	 * This method is having all subcommands of attackphase and it will call another
	 * methods accordingly
	 * 
	 * @param command
	 * @return message to console regarding what is happening
	 */
	private String attackPhase(String command) {

		String[] dataArray = command.split(" ");
		List<String> commandData = Arrays.asList(dataArray);
		if (commandData.get(0).equals("attack") && commandData.get(3).equals("-allout") && commandData.size() == 4
				&& validateInput(commandData.get(1), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
				&& validateInput(commandData.get(2), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {

			fromCountryAttack = commandData.get(1);
			toCountryAttack = commandData.get(2);
			if (riskPlayImpl.validateFromCountry(fromCountryAttack, playerList.get(playerIndex))
					&& riskPlayImpl.validateToCountry(fromCountryAttack, toCountryAttack, riskMap,
							playerList.get(playerIndex)))
			{
				attckAllOut(fromCountryAttack,toCountryAttack);
			}
			
		} else if (commandData.get(0).equals("attack") && commandData.get(1).equals("-noattack")) {
			sb.append(playerName + " ").append("decided not to attack anymore.").append(NEWLINE);
			sb.append("Attack phase ended.").append(NEWLINE);
			sb.append("Start with fortification commands").append(NEWLINE);
			attackphaseEnded = true;
		} else if (commandData.get(0).equals("attack") && validateInput(commandData.get(3), "[1-9][0-9]*")
				&& commandData.size() == 4 && validateInput(commandData.get(1), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
				&& validateInput(commandData.get(2), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
				&& (getint(commandData.get(3)) > 0 && getint(commandData.get(3)) <= 3)) {

			fromCountryAttack = commandData.get(1);
			toCountryAttack = commandData.get(2);
			attackerDice = Integer.parseInt(commandData.get(3));

			if (riskPlayImpl.validateFromCountry(fromCountryAttack, playerList.get(playerIndex))
					&& riskPlayImpl.validateToCountry(fromCountryAttack, toCountryAttack, riskMap,
							playerList.get(playerIndex))
					&& riskPlayImpl.validateAttackerDice(attackerDice, fromCountryAttack,
							playerList.get(playerIndex))) {

				defenderPlayer = riskPlayImpl.getPlayerByCountry(toCountryAttack, playerList);

				attackFire = true;

				PlayerTerritory toTerritory = defenderPlayer.getPlayerterritories().stream()
						.filter(x -> toCountryAttack.equals(x.getTerritoryName())).findAny().orElse(null);

				sb.append(defenderPlayer.getPlayerName() + "'s defend turn started..").append(NEWLINE);
				sb.append(defenderPlayer.getPlayerName() + "'s country Name: ").append(toCountryAttack)
						.append(" Army: ").append(toTerritory.getArmyOnterritory()).append(NEWLINE);

			} else {
				sb.append("Invalid attacker's fromCountry or toCountry or dies").append(NEWLINE);
				;
			}

		} else if (commandData.get(0).equals("defend") && validateInput(commandData.get(1), "[1-9][0-9]*")
				&& (getint(commandData.get(1)) > 0 && getint(commandData.get(1)) <= 2)) {

			defenderDice = getint(commandData.get(1));

			if (attackFire && riskPlayImpl.validateDefenderDice(defenderDice, toCountryAttack, defenderPlayer)) {

				attackFire = false;
				sb.append("Correct ==>").append(NEWLINE);

				decideBattle(attackerDice, defenderDice);
			} else {
				sb.append("Please do attack first or invalid defender dies").append(NEWLINE);
			}

		} else if (commandData.get(0).equals("attackmove") && validateInput(commandData.get(1), "[1-9][0-9]*")) {

		} else {
			sb.append("Please Enter Valid Command").append(NEWLINE);
		}
		return sb.toString();
	}

	/**
	 * @param fromCountry
	 * @param toCountry
	 */
	private void attckAllOut(String fromCountry, String toCountry) {
		boolean allOutTerritoryConqured = false;
		
		
	}

	/**
	 * This method will call another method to give generated random dice and after
	 * that this method will decide whos dice is more and update army accordingly.
	 * 
	 * @param attackerDice
	 * @param defenderDice
	 */
	private void decideBattle(int attackerDice, int defenderDice) {

		List<Integer> attackerList = riskPlayImpl.getCountFromDies(attackerDice);
		List<Integer> defenderList = riskPlayImpl.getCountFromDies(defenderDice);

		for (int i = 0; i < defenderList.size(); i++) {

			if (defenderList.get(i) == attackerList.get(i)) {
				updateArmyAfterBattle(fromCountryAttack, "attacker");
			} else if (defenderList.get(i) > attackerList.get(i)) {
				updateArmyAfterBattle(fromCountryAttack, "attacker");
			} else if (defenderList.get(i) < attackerList.get(i)) {
				updateArmyAfterBattle(toCountryAttack, "defender");
			}
		}
	}

	/**
	 * This method will update number of army from attcker or defender territory
	 * after battle completed everytime
	 * 
	 * @param country
	 * @param name
	 *            attcker or defender
	 */
	private void updateArmyAfterBattle(String country, String name) {

		for (Player player : playerList) {

			for (PlayerTerritory playerTerritory : player.getPlayerterritories()) {

				if (country.equalsIgnoreCase(playerTerritory.getTerritoryName())) {

					playerTerritory.setArmyOnterritory(playerTerritory.getArmyOnterritory() - 1);
					player.setArmyOwns(player.getArmyOwns() - 1);

					sb.append(name).append(" Country loses 1 army ").append(name).append(" has left with ")
							.append(playerTerritory.getArmyOnterritory()).append(NEWLINE);
					;

					fillTerritoryList();
					fillAdjacentTerritoryList();
					break;

				}
			}
		}
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		currentLog.setText("");
		currentLog.appendText("Current Player : " + gameplayphase.getPlayerList().get(playerIndex).getPlayerName());
		currentLog.appendText("Current Phase : " + gameplayphase.getGamePhase());
		currentLog.appendText("Armies Owned : " + gameplayphase.getPlayerList().get(playerIndex).getPlayerName());
	}

	/**
	 * This is a method for fortification process where player can move his/her army
	 * from one territory to it's adjacent owned territory.
	 * 
	 * @param command
	 *            is a fortification command given from player
	 * @return message of result from fortification process
	 */
	private String fortification(String command) {

		String[] dataArray = command.split(" ");
		List<String> commandData = Arrays.asList(dataArray);
		String fortificationMessage = null;

		if (commandData.get(0).equalsIgnoreCase("fortify")) {

			if (commandData.size() == 2 && commandData.get(0).equals("fortify") && commandData.get(1).equals("none")) {

				sb.append(playerName).append(" 's turn ended.!").append(NEWLINE);

				System.out.println("FORTIFY ====> " + playerList);

				fillTerritoryList();
				fillAdjacentTerritoryList();
				changeUserTurn();
			} else {

				if (commandData.size() != 4) {

					sb.append("Please Enter Valid command").append(NEWLINE);
				} else {
					if (validateInput(commandData.get(3), "[1-9][0-9]*")
							&& validateInput(commandData.get(1), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
							&& validateInput(commandData.get(2), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")) {

						String fromCountry = commandData.get(1);
						String toCountry = commandData.get(2);
						int armytoMove = Integer.parseInt(commandData.get(3));

						PlayerTerritory fromTerritory = playerList.get(playerIndex).getPlayerterritories().stream()
								.filter(x -> fromCountry.equals(x.getTerritoryName())).findAny().orElse(null);

						PlayerTerritory toTerritory = playerList.get(playerIndex).getPlayerterritories().stream()
								.filter(x -> toCountry.equals(x.getTerritoryName())).findAny().orElse(null);

						if (fromTerritory != null && toTerritory != null) {

							List<String> neighbourCountriesList = mapManagementImpl
									.getNeighbourCountriesListByCountryName(riskMap, fromCountry);

							System.out.println("neighbourCountriesList ==> " + neighbourCountriesList);

							if (neighbourCountriesList.contains(toCountry)) {

								if (fromTerritory.getArmyOnterritory() > armytoMove) {

									int left = fromTerritory.getArmyOnterritory() - armytoMove;

									System.out.println("left => " + left);

									fromTerritory.setArmyOnterritory(left);

									System.out.println(
											"from taratory army after => " + fromTerritory.getArmyOnterritory());

									System.out.println("To taratory before => " + toTerritory.getArmyOnterritory());

									int armyBefore = toTerritory.getArmyOnterritory();

									System.out.println("armyBefore=> " + armyBefore);

									toTerritory.setArmyOnterritory(armyBefore + armytoMove);

									int armyAfter = toTerritory.getArmyOnterritory();

									System.out.println("armyAfter=> " + armyAfter);

									sb.append(playerName).append("'s fortification done").append(NEWLINE);
									sb.append(playerName).append(" 's turn ended.!").append(NEWLINE);

									System.out.println("FORTIFY ====> " + playerList);

									fillTerritoryList();
									fillAdjacentTerritoryList();

									// Observer-Pattern Update Call
									fortificationMessage = armytoMove + " armies moved from " + fromTerritory + " to "
											+ toTerritory;

									changeUserTurn();

								} else {
									sb.append("you should have at least one army in your territory after fortification")
											.append(fromCountry).append(NEWLINE);
									fortificationMessage = sb.toString();
								}

							} else {
								sb.append(toCountry).append(" is not neighbour country of ").append(fromCountry)
										.append(NEWLINE);
								fortificationMessage = sb.toString();
							}

						} else {
							sb.append("fromcountry or tocountry not found : Please Enter Valid country Name")
									.append(NEWLINE);
							fortificationMessage = sb.toString();
						}

					} else {
						sb.append("Please Enter Valid command").append(NEWLINE);
						fortificationMessage = sb.toString();
					}

				}
			}
		} else {

			sb.append("Please Enter Valid command").append(NEWLINE);
			fortificationMessage = sb.toString();
		}

		// observerSubject.setFortificationMessage(fortificationMessage);
		return sb.toString();
	}

	@Override
	public void fortificationUpdate() {
		phaseviewLog.setText("");
		phaseviewLog.appendText(observerSubject.getFortificationMessage());
	}

	/**
	 * This method validate input given from user and return true if it's correct
	 * and false otherwise.
	 * 
	 * @param value
	 *            is string to be validated
	 * @param pattern
	 *            is regex
	 * @return true if validation got succeed.
	 */
	private boolean validateInput(String value, String pattern) {
		if (!value.isEmpty()) {
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(value);
			if (m.find() && m.group().equals(value)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * This a reinforcement method in which you can put some number of extra army on
	 * your desired territory each time your turn comes.
	 * 
	 * @param command
	 *            is a command fired from player
	 * @return message of result from reinforcement process
	 */
	private String placeReinforcement(String command) {
		String countryName = "";
		int armyToPlace;
		String[] dataArray = command.split(" ");
		List<String> commandData = Arrays.asList(dataArray);
		String reinforcementMessage;
		if (commandData.get(0).equals("reinforce")) {
			if (commandData.size() == 3 && validateInput(commandData.get(1), "^([a-zA-Z]-+\\s)*[a-zA-Z-]+$")
					&& validateInput(commandData.get(2), "[1-9][0-9]*")) {

				countryName = commandData.get(1);
				armyToPlace = Integer.parseInt(commandData.get(2));

				final String cName = countryName;

				PlayerTerritory playerTerritory = playerList.get(playerIndex).getPlayerterritories().stream()
						.filter(x -> cName.equals(x.getTerritoryName())).findAny().orElse(null);

				if (playerTerritory != null) {

					if (armyToPlace <= playerReinforceArmy && armyToPlace != 0 && playerReinforceArmy != 0) {

						System.out.println("playerTerritory before ==> " + playerTerritory);

						int army = playerTerritory.getArmyOnterritory() + armyToPlace;

						playerTerritory.setArmyOnterritory(army);

						System.out.println("playerTerritory After ==> " + playerTerritory);

						playerReinforceArmy = playerReinforceArmy - armyToPlace;

						String message = armyToPlace + " Assigned to " + cName;

						sb.append(message).append(NEWLINE);
						sb.append("you have left ").append(playerReinforceArmy).append(" to reinforcement")
								.append(NEWLINE);

						if (playerReinforceArmy == 0) {
							sb.append(playerName).append(" 's Reinforcement Phase done please go to the Attack phase")
									.append(NEWLINE);
						}

						System.out.println("REINFORCEMENT ====> " + playerList);
						fillTerritoryList();
						fillAdjacentTerritoryList();

					} else {
						sb.append("Please provide Valid Army details").append(NEWLINE).append("you have left ")
								.append(playerReinforceArmy).append(" to reinforcement").append(NEWLINE);
					}
				} else {
					sb.append("Country not found : Please Enter Valid country Name").append(NEWLINE);
				}
			}
		} else if (commandData.get(0).equals("exchangecards")) {
			if (commandData.get(1).equals("none")) {
				sb.append("Player Don't want to exchange cards. Please Enter attack commnads");
			} else {

			}

		} else {

			sb.append("Please Enter Valid Command").append(NEWLINE);
		}
		// Observer Pattern Update Call
		reinforcementMessage = sb.toString();
		// observerSubject.setReinforcementMessage(reinforcementMessage);
		return sb.toString();
	}

	@Override
	public void reinforcemrentUpdate() {

		phaseviewLog.setText("");
		phaseviewLog.appendText(observerSubject.getReinforcementMessage());
	}

	@Override
	public void attackUpdate() {

		phaseviewLog.setText("");
		phaseviewLog.appendText(observerSubject.getAttackMessage());
	}

	/**
	 * This method will exit the game terminates the window.
	 * 
	 * @param event
	 *            will represents value sent from view
	 */
	@FXML
	void exitGame(ActionEvent event) {
		stageManager.switchScene(FxmlView.WELCOME, null);
	}

	/**
	 * This method will transfer phase of playing from one player to another player
	 * for the first time
	 * 
	 * @param object
	 */
	public void transferGamePlayPhase(Object object) {

		playerList.clear();
		// Display the message
		gameplayphase = (GamePlayPhase) object;
		riskMap = mapManagementImpl.readMap(gameplayphase.getFileName());
		System.out.println("===> " + gameplayphase);
		System.out.println(riskMap);

		playerList.addAll(gameplayphase.getPlayerList());

		txtCommandLine.clear();
		txtConsoleLog.clear();

		playerName = playerList.get(playerIndex).getPlayerName();
		playerReinforceArmy = riskPlayImpl
				.checkForReinforcement(playerList.get(playerIndex).getPlayerterritories().size());

		turnStartedMsg = playerName + "'s turn is started";
		leftArmyMsg = "You have " + playerReinforceArmy + " armies to Reinforcement";
		sb.append(turnStartedMsg).append(NEWLINE).append(leftArmyMsg).append(NEWLINE);

		txtConsoleLog.setText(sb.toString());

		fillTerritoryList();
		fillAdjacentTerritoryList();
	}

	/**
	 * This method will change user's turn everytime
	 */
	private void changeUserTurn() {

		if (playerIndex < playerList.size() - 1) {
			playerIndex++;
			if (playerList.get(playerIndex).getPlayerName().equalsIgnoreCase(NEUTRAL)) {
				playerIndex = 0;
			}
		} else {
			playerIndex = 0;
		}

		txtCommandLine.clear();
		txtConsoleLog.clear();

		playerName = playerList.get(playerIndex).getPlayerName();
		playerReinforceArmy = riskPlayImpl
				.checkForReinforcement(playerList.get(playerIndex).getPlayerterritories().size());

		turnStartedMsg = playerName + "'s turn is started";
		leftArmyMsg = "You have " + playerReinforceArmy + " armies to Reinforcement";
		sb.append(turnStartedMsg).append(NEWLINE).append(leftArmyMsg).append(NEWLINE);
		txtConsoleLog.setText(sb.toString());

		fillTerritoryList();
		fillAdjacentTerritoryList();

	}

	/**
	 * This method will fill list of territories with it's army,that user owns to
	 * show it in GUI while he is having his turn
	 */
	private void fillTerritoryList() {

		territoryArea.clear();
		StringBuilder sbBuilder = new StringBuilder();
		Player player = playerList.get(playerIndex);
		if (player != null) {
			int count = 1;
			for (PlayerTerritory territory : player.getPlayerterritories()) {

				sbBuilder.append(count).append(") ").append(territory.getTerritoryName()).append(" : ")
						.append(territory.getArmyOnterritory()).append(NEWLINE);
				count++;
			}
			territoryArea.setText(sbBuilder.toString());
		}
	}

	/**
	 * This method will fill list of adjacent territories user owns to show it in
	 * GUI while he is having his turn
	 */
	private void fillAdjacentTerritoryList() {

		adjacentTerritoryArea.clear();
		StringBuilder sbBuilder = new StringBuilder();
		Player player = playerList.get(playerIndex);
		if (player != null) {

			List<PlayerTerritory> playerTerritories = player.getPlayerterritories();
			int count = 1;
			for (PlayerTerritory territory : playerList.get(playerIndex).getPlayerterritories()) {

				String country = territory.getTerritoryName();
				sbBuilder.append(count).append(") ").append(country).append("-").append(territory.getArmyOnterritory());
				List<String> neighbourCountriesList = mapManagementImpl.getNeighbourCountriesListByCountryName(riskMap,
						country);
				for (String neighbour : neighbourCountriesList) {

					PlayerTerritory playerTerritory = playerList.get(playerIndex).getPlayerterritories().stream()
							.filter(x -> neighbour.equals(x.getTerritoryName())).findAny().orElse(null);

					if (playerTerritory != null) {
						sbBuilder.append(" ==> ").append(playerTerritory.getTerritoryName());
					}

				}
				sbBuilder.append(NEWLINE);
				count++;
			}
			adjacentTerritoryArea.setText(sbBuilder.toString());
		}
	}

	private int getint(String str) {
		return Integer.parseInt(str);
	}

}
