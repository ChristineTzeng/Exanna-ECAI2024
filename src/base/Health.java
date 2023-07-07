package base;

public enum Health {
//    HEALTHY(0), ALLERGY(1), COVID(2); 
	NO_RISK(0), RISK(1);
    
	public int value;
	
	Health(int value) {
        this.value = value;
    }

	public static Health get(int value) {
        switch (value) {
//            case 0: return HEALTHY;
//            case 1: return ALLERGY;
//            case 2: return COVID;
            case 0: return NO_RISK;
            case 1: return RISK;
            default: return null;
        }
    }
	
	public int getValue() {
    	return value;
    }
}
