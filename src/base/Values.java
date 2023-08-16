package base;

/**
 * Defines values and their distributions
 */
public enum Values {
	HEALTH (0), FREEDOM (1);
	
	public int value;
	
	Values(int value) {
        this.value = value;
    }

	public static Values get(int value) {
        switch (value) {
	        case 0: return HEALTH;
	        case 1: return FREEDOM;
	        default: return null;
        }
    }
	
	public int getValue() {
    	return value;
    }

}
