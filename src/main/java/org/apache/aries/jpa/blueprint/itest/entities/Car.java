package org.apache.aries.jpa.blueprint.itest.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Car {
  @Id
  private String numberPlate;
  
  private String colour;
  
  private int engineSize;
  
  private int numberOfSeats;

  public String getNumberPlate() {
    return numberPlate;
  }

  public void setNumberPlate(String numberPlate) {
    this.numberPlate = numberPlate;
  }

  public String getColour() {
    return colour;
  }

  public void setColour(String colour) {
    this.colour = colour;
  }

  public int getEngineSize() {
    return engineSize;
  }

  public void setEngineSize(int engineSize) {
    this.engineSize = engineSize;
  }

  public int getNumberOfSeats() {
    return numberOfSeats;
  }

  public void setNumberOfSeats(int numberOfSeats) {
    this.numberOfSeats = numberOfSeats;
  }
  
  
}
