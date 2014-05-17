package uk.co.dezzanet.gametable.charactersheet;

import java.util.*;

/**
 * Character data - i.e. wounds, gold, notes etc
 * @author dezza
 *
 */
public class CharacterData {
	/**
	 * Max wounds for this character
	 */
	private int maximumWounds = 0;
	
	/**
	 * Current wounds for this character
	 */
	private int wounds = 0;
	
	/**
	 * Character notes
	 */
	private String notes = "";
	
	/**
	 * Current gold of the character
	 */
	private int gold = 0;
	
	/**
	 * @return the max wounds for this character
	 */
	public int getMaxWounds() {
		return maximumWounds;
	}
	
	/**
	 * Things which are listening to data change events
	 */
	List<ICharacterDataChangedListener> listeners = new ArrayList<ICharacterDataChangedListener>();
	
	public void addListener(ICharacterDataChangedListener toAdd) {
		listeners.add(toAdd);
	}
	
	/**
	 * Sets the max wounds
	 * @param int passed_wounds the wounds to set
	 */
	public void setMaxWounds(int passed_wounds) {
		maximumWounds = passed_wounds;
		if (wounds > maximumWounds) {
			wounds = maximumWounds;
		}
		notifyChange();
	}
	
	/**
	 * @return the wounds of this character
	 */
	public int getWounds() {
		return wounds;
	}
	
	/**
	 * Sets the wounds
	 * @param passed_wounds the wounds to set
	 */
	public void setWounds(int passed_wounds) {
		if (passed_wounds >= 0 && passed_wounds <= maximumWounds) {
			wounds = passed_wounds;
			notifyChange();
		}
	}
	
	/**
	 * @return the current gold of this character
	 */
	public int getGold() {
		return gold;
	}
	
	/**
	 * @param passed_gold the gold to set
	 */
	public void setGold(int passed_gold) {
		gold = passed_gold;
		notifyChange();
	}
	
	/**
	 * @return the current notes
	 */
	public String getNotes() {
		return notes;
	}
	
	/**
	 * Set the notes
	 * @param passed_notes
	 */
	public void setNotes(String passed_notes) {
		notes = passed_notes;
		notifyChange();
	}
	
	private void notifyChange() {
		for (ICharacterDataChangedListener listener : listeners) {
			listener.dataChanged();
		}
	}
}
