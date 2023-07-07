package base;

public enum Explanation {
	NN(0), NP(1), HN(2), HP(3);

    public int value;

    Explanation(int value) {
        this.value = value;
    }

    public static Explanation fromID(int value) {
        if (value == 0) return NN;
        else if (value == 1) return NP;
        else if (value == 2) return HN;
        else if (value == 3) return HP;
        return null;
    }
    
    public static Explanation get(int value) {
        switch (value) {
            case 0: return NN;
            case 1: return NP;
            case 2: return HN;
            case 3: return HP;
            default: return null;
        }
    }
    
    public int getValue() {
    	return value;
    }
}
