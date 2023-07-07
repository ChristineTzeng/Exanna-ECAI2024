package base;

public enum Relationship {
    FAMILY (0), FRIEND (1), COLLEAGUE (2), STRANGER (3);
    
    public int value;
	
    Relationship(int value) {
        this.value = value;
    }

	public static Relationship get(int value) {
        switch (value) {
	        case 0: return FAMILY;
	        case 1: return FRIEND;
	        case 2: return COLLEAGUE;
	        case 3: return STRANGER;
	        default: return null;
        }
    }
	
	public int getValue() {
    	return value;
    }
}
