import java.util.Random;

public class RAM {
	int[] freeFrameList;
	int totalFrame;
	int totalFreeFrame;
	Random rand;
	
	public RAM(int ramSize, int frameSize) {
		totalFrame = ramSize/frameSize;
		totalFreeFrame = totalFrame;
		freeFrameList = new int[totalFrame];
		rand = new Random();
	}
	
	int getFrameNum() {
		if(totalFreeFrame > 0) {
			int i = rand.nextInt(totalFreeFrame) + 1;
			for(int j = 0; j < totalFrame; j++) {
				if(freeFrameList[j] == 0) {
					i--;
				}
				if(i == 0) {
					freeFrameList[j] = 1;
					totalFreeFrame--;
					return j;
				}
			}
		}
		return -1;
	}
	
	void clear() {
		totalFreeFrame = totalFrame;
		freeFrameList = new int[totalFrame];
	}
}