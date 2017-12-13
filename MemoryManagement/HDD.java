import java.util.Random;

public class HDD {
	int[] freeSectorList;
	int totalSector;
	int totalFreeSector;
	Random rand;
	
	public HDD(int hddSize, int sectorSize) {
		totalSector = hddSize/sectorSize;
		totalFreeSector = totalSector;
		freeSectorList = new int[totalSector];
		rand = new Random();
	}

	int getSectorNum() {
		if(totalFreeSector > 0) {
			int i = rand.nextInt(totalFreeSector) + 1;
			for(int j = 0; j < totalSector; j++) {
				if(freeSectorList[j] == 0) {
					i--;
				}
				if(i == 0) {
					freeSectorList[j] = 1;
					totalFreeSector--;
					return j;
				}
			}
		}
		return -1; // disk is full
	}
	
	void clear() {
		totalFreeSector = totalSector;
		freeSectorList = new int[totalSector];
	}
}