package simulation;

import java.util.Objects;

import base.Location;
import base.Norm;

/**
 * Defines agent types and associated value preferences
 */
public enum AgentType {
    HEALTH(0, new double[]{1.0, 0.0}, "HEALTH"),
    FREEDOM(1, new double[]{0.0, 1.0}, "FREEDOM");

	private final int value;
    public final double[] weights;
    public String name;
    
    AgentType(int value, double[] weights, String name) {
        this.value = value;
        this.weights = weights;
        this.name = name;
    }
    
    AgentType(int value, double[] weights) {
        this.value = value;
        this.weights = weights;
    }
    
    public int getValue() {
        return value;
    }
    
    public static AgentType get(int value) {
        switch (value) {
	        case 0: return HEALTH;
	        case 1: return FREEDOM;
	        
	        default: return null;
        }
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}