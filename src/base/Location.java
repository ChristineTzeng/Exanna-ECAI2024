package base;

public enum Location {
    HOME (0), OFFICE (1), PARTY (2), PARK (3), HOSPITAL (4);
    
	public int value;
	
	Location(int value) {
        this.value = value;
    }

	public static Location get(int value) {
        switch (value) {
	        case 0: return HOME;
	        case 1: return OFFICE;
	        case 2: return PARTY;
	        case 3: return PARK;
	        case 4: return HOSPITAL;
	        
	        default: return null;
        }
    }
	
	public int getValue() {
    	return value;
    }
}
