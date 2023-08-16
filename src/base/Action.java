package base;

/**
 * Defines the action space
 */
public enum Action {
	WEAR(0), NOT_WEAR(1);

    public int value;

    Action(int value) {
        this.value = value;
    }

    public static Action fromID(int value) {
        if (value == 1) return NOT_WEAR;
        return WEAR;
    }
    
    public int getValue() {
    	return value;
    }
}
