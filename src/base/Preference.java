package base;

/**
 * Defines personal preferences
 */
public enum Preference {
    WEAR (0), NOT_WEAR (1);
    
    public int value;
	
    Preference(int value) {
        this.value = value;
    }

	public static Preference get(int value) {
        switch (value) {
	        case 0: return WEAR;
	        case 1: return NOT_WEAR;
	        default: return WEAR;
        }
    }
	
	public int getValue() {
    	return value;
    }
}
