package beans.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Laptop {

  @Id
  private String serialNumber;
  
  private int numberOfCores;
  
  private int hardDiskSize;

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public int getNumberOfCores() {
    return numberOfCores;
  }

  public void setNumberOfCores(int numberOfCores) {
    this.numberOfCores = numberOfCores;
  }

  public int getHardDiskSize() {
    return hardDiskSize;
  }

  public void setHardDiskSize(int hardDiskSize) {
    this.hardDiskSize = hardDiskSize;
  }
  
  
  
}
