/**
 * CS 6560
 * Project #2
 * Date: 12042017
 * Name: Hongjie Zhu
 * NetID: hf6233
 * OS: Windows 10
 * IDE: Eclipse Luna
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

public class Main {
	int verbose = 1;
	// set constants
	int HDDSIZE = 1048576; // 1M
	int VMSIZE = 16384; // 16k
	int RAMSIZE = 1024; // 1k
	int PAGESIZE = 16; 
	int FRAMESIZE = PAGESIZE; 
	int SECTORSIZE = PAGESIZE;
	int MAXPAGENUM = 100;	
	int totalFrame = RAMSIZE/FRAMESIZE;
	int totalSector = HDDSIZE/SECTORSIZE;
	// user inputs
	int totalProgramPage;
	int maxFrameAllowed;
	int flag = 0;
	// PC used to randomly generate the logical address
	int pc;
	int pageNumLength = Integer.toBinaryString(VMSIZE/PAGESIZE - 1).length();
	int offsetLength = Integer.toBinaryString(PAGESIZE - 1).length();
	// RAM and HDD used to "randomly" generate the frame# and sector#
	RAM ram;
	HDD hdd;
	// VPTR
	String[] vptr;
	int pageNumBegin = 0;
	int pageNumEnd = pageNumBegin + pageNumLength;
	int frameNumBegin = pageNumEnd;
	int frameNumEnd = frameNumBegin + Integer.toBinaryString(totalFrame - 1).length();
	int sectorNumBegin = frameNumEnd;
	int sectorNumEnd = sectorNumBegin + Integer.toBinaryString(totalSector - 1).length();
	int vaildInvaildBitBegin = sectorNumEnd;
	int vaildInvaildBitEnd = vaildInvaildBitBegin + 1;
	int dirtyBitBegin = vaildInvaildBitEnd;
	int dirtyBitEnd = dirtyBitBegin + 1;
	int countBegin = dirtyBitEnd;
	int maxCount = 128;
	int countEnd = countBegin + Integer.toBinaryString(maxCount - 1).length();
	int timeBegin = countEnd;
	int maxTime = 16384;
	int timeEnd = timeBegin + Integer.toBinaryString(maxTime - 1).length();
	String empty = String.format("%" + timeEnd + "s", "");
	
	Random rand;
	
	public Main() { // constructor
		ram = new RAM(RAMSIZE, FRAMESIZE);
		hdd = new HDD(HDDSIZE, SECTORSIZE);
		vptr = new String[MAXPAGENUM];
		for(int i = 0; i < MAXPAGENUM; i++) {
			vptr[i] = empty;
		}
		rand = new Random();
	}

	void cmd(String[] args, Scanner scanner) { // parse user input
		if(args.length > 0) {
			switch(args[0].toUpperCase()) {
			case "NEW": // ask for the pages the program has and the maximum frames allowed
				if(args.length == 1) {
					ram.clear();
					hdd.clear();
					for(int i = 0; i < MAXPAGENUM; i++) {
						vptr[i] = empty;
					}
					do {
						System.out.print("How many pages does this program have?> ");
						int userInput = Integer.parseInt(scanner.nextLine());
						if(userInput > MAXPAGENUM) {
							System.out.println("Maximum pages allowed in this simulation is " + MAXPAGENUM + ", please enter a number between 1 to " + MAXPAGENUM + "!");
							continue;
						}
						else {
							totalProgramPage = userInput;
							break;
						}
					} while(true);
					do {
						System.out.print("How many frames are allowed for this program?> ");
						int userInput = Integer.parseInt(scanner.nextLine());
						if(userInput > totalFrame) {
							System.out.println("Maximum frames allowed in this simulation is " + totalFrame + ", please enter a number between 1 to " + totalFrame + "!");
							continue;
						}
						else {
							maxFrameAllowed = userInput;
							break;
						}
					} while(true);
					flag = 1;
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "SET": // change the maximum frames allowed
				if(args.length == 1) {
					do {
						System.out.print("How many frames are allowed for this program?> ");
						int userInput = Integer.parseInt(scanner.nextLine());
						if(userInput > totalFrame) {
							System.out.println("Maximum frames allowed in this simulation is " + totalFrame + ", please enter a number between 1 to " + totalFrame + "!");
							continue;
						}
						else {
							maxFrameAllowed = userInput;
							break;
						}
					} while(true);
					int validFrameCount = validFrameCount();
					if(validFrameCount > maxFrameAllowed) {
						for(int i = validFrameCount - maxFrameAllowed; i > 0; i--) {
							int j = pickVictimFrame();
							vptr[j] = vptr[j].substring(0, vaildInvaildBitBegin) + "0" + vptr[j].substring(vaildInvaildBitEnd);
							vptr[j] = vptr[j].substring(0, timeBegin) + String.format("%" + (timeEnd - timeBegin) + "s", Integer.toBinaryString(0)).replace(' ', '0') + vptr[j].substring(timeEnd);
							vptr[j] = vptr[j].substring(0, frameNumBegin) + String.format("%" + (frameNumEnd - frameNumBegin) + "s", "") + vptr[j].substring(frameNumEnd);
						}
					}
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "RUN": // run simulation for a specific number of times
				if(args.length == 2) {
					if(flag == 1) {
						int loopCount = Integer.parseInt(args[1]);
						if(loopCount > 0) {
							run(loopCount);
						}
					}
					else {
						System.out.println("Maximum pages allowed and maximum frames allowed have not been set, use 'NEW' to set!");
					}
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "PRINT": // print VPTR
				if(args.length == 1) {
					print();
				}
				else if (args.length == 2) {
					print(args[1].toUpperCase());
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			default:
				System.out.println("Invalid command!"); // invalid command
			}
		}
	}
	
	void run(int loopCount) {
		for(; loopCount > 0; loopCount--) {
			pc = rand.nextInt(totalProgramPage * PAGESIZE);
			String pcBin = String.format("%" + (pageNumLength + offsetLength) + "s", Integer.toBinaryString(pc)).replace(' ', '0');
			String offsetBin = pcBin.substring(pcBin.length() - offsetLength, pcBin.length());
			String pageNumBin = pcBin.substring(0, pcBin.length() - offsetLength);
			int i = reference(pageNumBin, offsetBin);
			if(verbose == 1) {
				int offset = Integer.parseUnsignedInt(offsetBin, 2);
				int pageNum = Integer.parseUnsignedInt(pageNumBin, 2);
				String frameNumBin = vptr[i].substring(frameNumBegin, frameNumEnd);
				int frameNum = Integer.parseUnsignedInt(frameNumBin, 2);
				int pa = Integer.parseUnsignedInt(frameNumBin + offsetBin, 2);
				System.out.println(" @" + i + " -> ["
						+ "LA: " + pc 
						+ " P#: " + pageNum 
						+ " Offset: " + offset 
						+ " F#: " + frameNum
						+ " Offset: " + offset 
						+ " PA: " + pa
						+ " S#: " + Integer.parseUnsignedInt(vptr[i].substring(sectorNumBegin, sectorNumEnd), 2)
						+ "]");
				print("V");
				System.out.println();
			}
		}
	}

	int reference(String pageNumBin, String offsetBin) {
		for(int i = 0; i < MAXPAGENUM; i++) {
			if(vptr[i].substring(pageNumBegin, pageNumEnd).equals(pageNumBin)) {
				if(vptr[i].charAt(vaildInvaildBitBegin) == '1') { // page is in memory, hit
					// update count or time
					int count = Integer.parseUnsignedInt(vptr[i].substring(countBegin, countEnd), 2);
					if(count < maxCount) {
						count++;
					}
					vptr[i] = vptr[i].substring(0, countBegin) + String.format("%" + (countEnd - countBegin) + "s", Integer.toBinaryString(count)).replace(' ', '0') + vptr[i].substring(countEnd);
					increaseAllTime();
					if(verbose == 1) {
						System.out.print("Ref, hit");
					}
					return i;
				}
				else { // page is not in memory, miss
					referenceHelper(i);
					increaseAllTime();
					if(verbose == 1) {
						System.out.print("Ref, miss");
					}
					return i;
				}
			}
		}
		// if page# is not in the VPTR yet
		int freeSlot = getEmptySlot(); // randomly generate an index of an empty slot on the VPTR 
		if(freeSlot != -1) {
			vptr[freeSlot] = pageNumBin + vptr[freeSlot].substring(pageNumEnd);
			vptr[freeSlot] = vptr[freeSlot].substring(0, sectorNumBegin) + String.format("%" + (sectorNumEnd - sectorNumBegin) + "s", Integer.toBinaryString(hdd.getSectorNum())).replace(' ', '0') + vptr[freeSlot].substring(sectorNumEnd);
			referenceHelper(freeSlot);
			increaseAllTime();
			if(verbose == 1) {
				System.out.print("Ref, new");
			}
			return freeSlot;
		}
		return -1;
	}

	int getEmptySlot() { // "randomly" return the index of a free slot on the VPTR
		int freeSlotCount = 0;
		for(int i = 0; i < MAXPAGENUM; i++) {
			if(vptr[i].equals(empty)) {
				freeSlotCount++;
			}
		}
		if(freeSlotCount > 0) {
			int i = rand.nextInt(freeSlotCount) + 1;
			for(int j = 0; j < MAXPAGENUM; j++) {
				if(vptr[j].equals(empty)) {
					i--;
				}
				if(i == 0) {
					return j;
				}
			}
		}
		return -1;
	}
	
	void referenceHelper(int i) {
		int validFrameCount = validFrameCount();
		if(validFrameCount < maxFrameAllowed) { // placement	
			resetAllCount();
			vptr[i] = vptr[i].substring(0, frameNumBegin) + String.format("%" + (frameNumEnd - frameNumBegin) + "s", Integer.toBinaryString(ram.getFrameNum())).replace(' ', '0') + vptr[i].substring(frameNumEnd);
		}
		else { // replacement
			resetAllCount();
			int j = pickVictimFrame();
			vptr[j] = vptr[j].substring(0, vaildInvaildBitBegin) + "0" + vptr[j].substring(vaildInvaildBitEnd);
			vptr[j] = vptr[j].substring(0, dirtyBitBegin) + "0" + vptr[j].substring(dirtyBitEnd);
			vptr[j] = vptr[j].substring(0, timeBegin) + String.format("%" + (timeEnd - timeBegin) + "s", Integer.toBinaryString(0)).replace(' ', '0') + vptr[j].substring(timeEnd);
			vptr[i] = vptr[i].substring(0, frameNumBegin) + vptr[j].substring(frameNumBegin, frameNumEnd) + vptr[i].substring(frameNumEnd);
			vptr[j] = vptr[j].substring(0, frameNumBegin) + String.format("%" + (frameNumEnd - frameNumBegin) + "s", "") + vptr[j].substring(frameNumEnd);
		}
		vptr[i] = vptr[i].substring(0, vaildInvaildBitBegin) + "1" + vptr[i].substring(vaildInvaildBitEnd);
		vptr[i] = vptr[i].substring(0, dirtyBitBegin) + Integer.toString(rand.nextInt(2)) + vptr[i].substring(dirtyBitEnd);
		vptr[i] = vptr[i].substring(0, countBegin) + String.format("%" + (countEnd - countBegin) + "s", Integer.toBinaryString(1)).replace(' ', '0') + vptr[i].substring(countEnd);
		vptr[i] = vptr[i].substring(0, timeBegin) + String.format("%" + (timeEnd - timeBegin) + "s", Integer.toBinaryString(0)).replace(' ', '0') + vptr[i].substring(timeEnd);
	}
	
	int validFrameCount() {
		int validFrameCount = 0;
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				validFrameCount++;
			}
		}
		return validFrameCount;
	}
	
	int pickVictimFrame() {
		int i = rand.nextInt(MAXPAGENUM);
		int count;
		int minCount = maxCount;
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				count = Integer.parseUnsignedInt(vptr[j].substring(countBegin, countEnd), 2);
				if(count < minCount) {
					minCount = count;
				}
			}
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				count = Integer.parseUnsignedInt(vptr[j].substring(countBegin, countEnd), 2);
				if(count == minCount) {
					list.add(j);
				}
			}
		}
		Collections.shuffle(list);
		Iterator<Integer> itr=list.iterator();
		int time;
		int maxTime = 0;
		while(itr.hasNext()) {
			int j = itr.next();
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				time = Integer.parseUnsignedInt(vptr[j].substring(timeBegin, timeEnd), 2);
				if(time > maxTime) {
					maxTime = time;
					i = j;
				}
			}
		}
		return i;
	}
	
	void resetAllCount() { // was called every time bring in a new page
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				vptr[j] = vptr[j].substring(0, countBegin) + String.format("%" + (countEnd - countBegin) + "s", Integer.toBinaryString(0)).replace(' ', '0') + vptr[j].substring(countEnd);
			}
		}
	}
	
	void increaseAllTime() { // was called every time reference a page
		int time;
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(vptr[j].charAt(vaildInvaildBitBegin) == '1') {
				time = Integer.parseUnsignedInt(vptr[j].substring(timeBegin, timeEnd), 2);
				if(time < maxTime) {
					time++;
				}
				vptr[j] = vptr[j].substring(0, timeBegin) + String.format("%" + (timeEnd - timeBegin) + "s", Integer.toBinaryString(time)).replace(' ', '0') + vptr[j].substring(timeEnd);
			}
		}
	}
	
	void print() {	// print the raw VPTR
		for(int j = 0; j < MAXPAGENUM; j++) {
			System.out.println(vptr[j]);
		}
	}
	
	void print(String s) { // print the VPTR in selected format
		switch(s) {
		case "B": // print the whole VPTR in binary but add space as delimiter
			System.out.println("Index\tP#\tF#\t\tS#\tV/I Dirty Count\tTime");
			for(int j = 0; j < MAXPAGENUM; j++) {
				System.out.printf("%2d: ", j);
				System.out.println(vptr[j].substring(pageNumBegin, pageNumEnd)
						+ " " + vptr[j].substring(frameNumBegin, frameNumEnd)
						+ " " + vptr[j].substring(sectorNumBegin, sectorNumEnd)
						+ " " + vptr[j].substring(vaildInvaildBitBegin, vaildInvaildBitEnd)
						+ " " + vptr[j].substring(dirtyBitBegin, dirtyBitEnd)
						+ " " + vptr[j].substring(countBegin, countEnd)
						+ " " + vptr[j].substring(timeBegin, timeEnd));
			}
			break;
		case "D": // print the whole VPTR in decimal
			printHelper('D');
			break;
		case "V": // only print the VPTR entries that have page#
			printHelper('V'); 
			break;
		default:
			System.out.println("Invalid parameter! Will print the raw table.");
			print();
		}
	}
	
	void printHelper(char c) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		System.out.println("Index\tP#\tF#\tS#\tV/I\tDirty\tCount\tTime");
		for(int j = 0; j < MAXPAGENUM; j++) {
			if(c == 'V' && vptr[j].substring(pageNumBegin, pageNumEnd).equals(String.format("%" + (pageNumEnd - pageNumBegin) + "s", ""))) {
				// skip empty entry
			}
			else {
				list.add(j);
			}
		}
		Iterator<Integer> itr=list.iterator();
		while(itr.hasNext()) {
			int j = itr.next();
			String pageNumBin = vptr[j].substring(pageNumBegin, pageNumEnd);
			String frameNumBin = vptr[j].substring(frameNumBegin, frameNumEnd);
			String sectorNumBin = vptr[j].substring(sectorNumBegin, sectorNumEnd);
			String countBin = vptr[j].substring(countBegin, countEnd);
			String timeBin = vptr[j].substring(timeBegin, timeEnd);
			System.out.printf("%2d:\t", j);
			if(pageNumBin.equals(String.format("%" + (pageNumEnd - pageNumBegin) + "s", ""))) {
				System.out.print(pageNumBin);
			}
			else {
				System.out.print(Integer.parseUnsignedInt(pageNumBin, 2));
			}
			System.out.print("\t");
			if(frameNumBin.equals(String.format("%" + (frameNumEnd - frameNumBegin) + "s", ""))) {
				System.out.print(frameNumBin);
			}
			else {
				System.out.print(Integer.parseUnsignedInt(frameNumBin, 2));
			}
			System.out.print("\t");
			if(sectorNumBin.equals(String.format("%" + (sectorNumEnd - sectorNumBegin) + "s", ""))) {
				System.out.print(sectorNumBin);
			}
			else {
				System.out.print(Integer.parseUnsignedInt(sectorNumBin, 2));
			}
			System.out.print("\t");
			System.out.print(vptr[j].substring(vaildInvaildBitBegin, vaildInvaildBitEnd));
			System.out.print("\t");
			System.out.print(vptr[j].substring(dirtyBitBegin, dirtyBitEnd));
			System.out.print("\t");
			if(countBin.equals(String.format("%" + (countEnd - countBegin) + "s", ""))) {
				System.out.print(countBin);
			}
			else {
				System.out.print(Integer.parseUnsignedInt(countBin, 2));
			}
			System.out.print("\t");
			if(timeBin.equals(String.format("%" + (timeEnd - timeBegin) + "s", ""))) {
				System.out.print(timeBin);
			}
			else {
				System.out.print(Integer.parseUnsignedInt(timeBin, 2));
			}
			System.out.println();
		}
		
	}
	
	public static void main(String[] args) {
		Main simulator = new Main();
		Scanner scanner = new Scanner(System.in);
		do {
			System.out.print("> ");
			String str = scanner.nextLine();
			args = str.split("\\s+");
			if(args[0].toUpperCase().equals("EXIT") ) {
				break;
			}
			simulator.cmd(args, scanner);
		} while(true);
		scanner.close();
	}
}
