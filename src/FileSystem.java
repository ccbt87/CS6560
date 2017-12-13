import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileSystem {
	int verbose = 1;
	
	int totalSectors;
	char[][] disk;
	int[] freeSectorList;
	// define sector structure
	int backPos = 0;
	int frwdPos = 4;
	// structure for directory sector
	int dirPos = 16;
	int typePos = dirPos;
	int nameOffset = 1;
	int linkOffset = 10;
	int sizeOffset = 14;
	// structure for data sector
	int dataPos = 8;
	// length of each entry in sector structure
	int backSize = 4;
	int frwdSize = 4;
	// length of each entry in directory sector
	int typeSize = 1;
	int nameSize = 9;
	int linkSize = 4;
	int sizeSize = 2;
	// length of each entry in data sector
	int dataSize = 504;
	// define other variables
	Stack<String> path;
	Stack<Integer> currentDir;
	int currentSector;
	char openMode;
	int fileEntry;
	int filePointer;	// range 8-511
	int lastSize;		// range 0-504
	int seekFlag;
	int numOfDirSectors;
	int numOfDataSectors;
	int headSector;
	int tailSector;
	
	// Constructor
	public FileSystem(int tSectors) {
		totalSectors = tSectors;
		disk = new char[totalSectors][512];
		freeSectorList = new int[totalSectors];
		path = new Stack<String>();
		currentDir = new Stack<Integer>();

		path.push("/");
		currentDir.push(0);
		freeSectorList[0] = 1;
		currentSector = 0;
		openMode = 'X';
	}

	void create(char type, String name) {
		if(openMode == 'X') {
			if(type == 'U' || type == 'D') {
				//check existence and overwrite if exists
				delete(name); 
				// else file does not exist
				String[] nArr = name.split("/");
				if(nArr.length > 0) {
					if(nArr.length > 1) {
						String n = String.join("/", Arrays.copyOf(nArr, nArr.length-1));
						if(cd(n) == -1) {
							create('D', n);
							cd(n);
						}
					}
					int entryNum = firstFreeDirEntry();
					int freeSector = firstFreeSector();
					if(freeSector != -1 && entryNum != -1) {
						if(type == 'U') {
							disk[currentSector][typePos * entryNum] = 'U';
						}
						else if(type == 'D') {
							disk[currentSector][typePos * entryNum] = 'D';
						}
						int i = dirPos * entryNum + nameOffset;
						for(char c : String.format("%-9s", nArr[nArr.length-1]).toCharArray()) {
							disk[currentSector][i] = c;
							i++;
						}
						char[] arr2 = new char[2];
						char[] arr = new char[4];
						arr = intToCharArr(freeSector, 4);
						for(int j = 0; j < 4; j++) {
							disk[currentSector][dirPos * entryNum + linkOffset + j] = arr[j];
						}
						arr2 = intToCharArr(0, 2);
						for(int j = 0; j < 2; j++) {
							disk[currentSector][dirPos * entryNum + sizeOffset + j] = arr2[j];
						}
						if(verbose == 1) {
							System.out.println("Created at sector " + freeSector);
						}
						if(type == 'U') {
							open('O', nArr[nArr.length-1]);
						}
					}
				}
			}
			else {
				System.out.println("Invalid type " + type);
			}
		}
		else {
			if(verbose == 1) {
				System.out.println("A file is still open, couldn't create new file.");
			}
		}
	}

	int open(char mode, String name) {
		if(openMode == 'X') {
			String[] nArr = name.split("/");
			if(nArr.length > 0) {
				if(nArr.length > 1) {
					if(cd(String.join("/", Arrays.copyOf(nArr, nArr.length-1))) == -1) {
						return -1;
					}
				}
				fileEntry = searchName(nArr[nArr.length-1]);
				if(fileEntry != -1) {
					if(disk[currentDir.peek()][typePos * fileEntry] == 'U') {
						char[] arr2 = new char[2];
						for(int i = 0; i < 2; i++) {
							arr2[i] = disk[currentSector][dirPos * fileEntry + sizeOffset + i];
						}
						lastSize = charArrToInt(arr2);
						char[] arr = new char[4];
						for(int i = 0; i < 4; i++) {
							arr[i] = disk[currentSector][dirPos * fileEntry + linkOffset + i];
						}
						currentSector = charArrToInt(arr);
						// find head and tail sector
						headSector = currentSector;
						tailSector = currentSector;
						int frwdSector;
						do {
							for(int i = 0; i < 4; i++) {
								arr[i] = disk[tailSector][frwdPos + i];
							}
							frwdSector = charArrToInt(arr);
							if(frwdSector != 0) {
								tailSector = frwdSector;
							}
						} while(frwdSector != 0);
						openMode = mode;
						if(openMode == 'O') {
							openMode = 'I';
							seekFlag = seek(1, 1);
							if(seekFlag == -1) { // disk is full, could not open file in O mode
								currentSector = currentDir.peek();
								openMode = 'X';
							}
							else {
								if(seekFlag == 1) {
									lastSize++;
								}
								else if(seekFlag == 3) {
									lastSize = 1;
								}
								openMode = 'O';
							}
						}
						else if(openMode == 'I' || openMode == 'U') {
							seek(-1, 0);
						}
					}
					else {
						if(verbose == 1) {
							System.out.println("File is a directory, use cd instead of open.");
						}
					}
				}
				else { // else file does not exist
					if(verbose == 1) {
						System.out.println("File doesn't exist.");
					}
				}
			}
		}
		else {
			if(verbose == 1) {
				System.out.println("A file is still open, couldn't open new file.");
			}
		}
		return 0;
	}

	void close() {
		if(openMode != 'X') {
			if(seekFlag == 3) {
				char[] arr = new char[4];
				for(int j = 0; j < 4; j++) {
					arr[j] = disk[tailSector][backPos + j];
				}
				freeSectorList[tailSector] = 0;
				tailSector = charArrToInt(arr);
				arr = intToCharArr(0, 4);
				for(int j = 0; j < 4; j++) {
					disk[tailSector][frwdPos + j] = arr[j];
				}
			}
			currentSector = currentDir.peek();
			openMode = 'X';
			if(verbose == 1) {
				System.out.println("Closed.");
			}
		}
		else {
			if(verbose == 1) {
				System.out.println("No file is open.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	int delete(String name) {
		if(openMode == 'X') {
			String[] nArr = name.split("/");
			if(nArr.length > 0) {
				Stack<String> pathBackup;
				Stack<Integer> currentDirBackup;
				currentDirBackup = (Stack<Integer>) currentDir.clone();
				pathBackup = (Stack<String>) path.clone();
				if(nArr.length > 1) {
					if(cd(String.join("/", Arrays.copyOf(nArr, nArr.length-1))) == -1) {
						return -1;
					}
				}
				int fileEntry = searchName(nArr[nArr.length-1]);
				if(fileEntry != -1) {
					if(disk[currentSector][typePos * fileEntry] == 'U') {
						disk[currentSector][typePos * fileEntry] = 'F';
					}
					else if(disk[currentSector][typePos * fileEntry] == 'D') {
						char[] arr = new char[4];
						for(int i = 0; i < 4; i++) {
							arr[i] = disk[currentSector][dirPos * fileEntry + linkOffset + i];
						}
						deleteHelper(charArrToInt(arr));
						disk[currentSector][typePos * fileEntry] = 'F';
					}	
					garbageCollection();
					if(verbose == 1) {
						System.out.println("File entry " + fileEntry + " deleted.");
					}
				}
				else {// else file does not exist
					if(verbose == 1) {
						System.out.println("File doesn't exist.");
					}
				}
				path = pathBackup;
				currentDir = currentDirBackup;
				currentSector = currentDir.peek();
			}
		}
		else {
			if(verbose == 1) {
				System.out.println("A file is still open, couldn't delete.");
			}
		}
		return 0;
	}

	void deleteHelper(int currSector) {
		char[] arr = new char[4];
		int frwdSector;
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currSector][typePos * i] == 'D') {	
					for(int j = 0; j < 4; j++) {
						arr[j] = disk[currSector][dirPos * i + linkOffset + j];
					}
					deleteHelper(charArrToInt(arr));
					disk[currSector][typePos * i] = 'F';
				}
				else if(disk[currSector][typePos * i] == 'U') {
					disk[currSector][typePos * i] = 'F';
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
			}			
		} while(frwdSector != 0);
	}
	
	void read(int n) {
		if(openMode == 'I' || openMode == 'U') {
			for(int i = 0; i < n; i++) {
				System.out.print(disk[currentSector][filePointer]);
				if(seek(0, 1) == 1) {
					System.out.print("\nEOF reached!");
					break;
				}
			}
			System.out.println("");
		}
		else {
			if(verbose == 1) {
				if(openMode == 'O') {
					System.out.println("read is not permitted in O mode");
				}
				else {
					System.out.println("open a file first");
				}
			}			
		}
	}

	int write(int n, char[] data) {
		if(openMode == 'O' || openMode == 'U') {
			if(n > 0) {
				if(openMode == 'U') {
					lastSize++;
				}
				for(int i = 0; i < n; i++) {
					if(i < data.length) {
						disk[currentSector][filePointer] = data[i];
					}
					else {
						disk[currentSector][filePointer] = ' ';
					}
					if(openMode == 'O') {
						openMode = 'I';
						seekFlag = seek(0, 1);
						openMode = 'O';
					}
					else { // else U mode
						seekFlag = seek(0, 1);
					}
					if(seekFlag == -1) {
						break;
					}
					else if(seekFlag == 1) {
						lastSize++;
					}
					else if(seekFlag == 3) {
						lastSize = 1;
					}
				}
				// finished writing, update size
				char[] arr = new char[2];
				if(openMode == 'U') {
					lastSize = lastSize - 1;
					arr = intToCharArr(lastSize, 2);
				}
				else {
					arr = intToCharArr(lastSize-1, 2);
				}			
				for(int i = 0; i < 2; i++) {
					disk[currentDir.peek()][dirPos * fileEntry + sizeOffset + i] = arr[i];
				}
			}
		}
		else {
			if(verbose == 1) {
				if(openMode == 'I') {
					System.out.println("write is not permitted in I mode");
				}
				else {
					System.out.println("open a file first");
				}
			}			
		}
		return 0;
	}
	
	int seek(int base, int offset) {
		if(openMode == 'I' || openMode == 'U') {
			if(base == -1) {
				currentSector = headSector;
				filePointer = dataPos;
			}
			else if(base == 1) {
				currentSector = tailSector;
				filePointer = dataPos + lastSize - 1;
			}
			else if(base == 0) {
				// 
			}
			else {
				System.out.println("Invalid base " + base + " use default base 0");
			}
			while(offset != 0) {
				if(offset > 0) {
					if(currentSector == tailSector) {
						if(filePointer >= dataPos + lastSize - 1) { // if filePointer right before or after EOF
							if(filePointer == dataPos + lastSize - 1) { // if filePointer right before EOF
								if(filePointer == dataPos + dataSize - 1) { // if filePointer == 511
									int freeSector = firstFreeSector();
									if(freeSector == -1) {
										return -1;
									}
									else {
										char[] arr = new char[4];
										arr = intToCharArr(freeSector, 4);
										for(int j = 0; j < 4; j++) {
											disk[currentSector][frwdPos + j] = arr[j];
										}
										arr = intToCharArr(currentSector, 4);
										for(int j = 0; j < 4; j++) {
											disk[freeSector][backPos + j] = arr[j];
										}
										currentSector = tailSector = freeSector;
										filePointer = dataPos;
										return 3; // use new sector
									}
								}
								else { // if filePointer < 511
									filePointer++;
									offset--;
									return 1; // right after the EOF
								}
							}
							else {
								return 1; // right after the EOF
							}
						}
						else { // if filePointer < EOF
							filePointer++;
							offset--;
						}
					}
					else { // if currentSector != tailSector
						if(filePointer == dataPos + dataSize - 1) {
							int frwdSector;
							char[] arr = new char[4];
							for(int i = 0; i < 4; i++) {
								arr[i] = disk[currentSector][frwdPos + i];
							}
							frwdSector = charArrToInt(arr);
							if(frwdSector != 0) {
								currentSector = frwdSector;
							}
							filePointer = dataPos;
						}
						else { // if filePointer < 511
							filePointer++;							
						}						
						offset--;
					}
				}
				else {	// if offset < 0
					if(filePointer == dataPos) {
						if(currentSector == headSector) {
							return 2; // head of file reached
						}
						else {
							int backSector;
							char[] arr = new char[4];
							for(int i = 0; i < 4; i++) {
								arr[i] = disk[currentSector][backPos + i];
							}
							backSector = charArrToInt(arr);
							if(backSector != 0) {
								currentSector = backSector;
							}
							filePointer = dataPos + dataSize - 1;
						}
					}
					else { // if filePointer > 8
						filePointer--;
					}
					offset++;
				}
			}
			return 0;
		}
		else {
			if(verbose == 1) {
				if(openMode == 'I') {
					System.out.println("seek is not permitted in O mode");
				}
				else {
					System.out.println("open a file first");
				}
			}			
		}
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	int cd(String name) {
		if(openMode == 'X') {
			if(name.length() > 0) {
				String[] nArr = name.split("/");
				if(nArr.length == 0) {
					currentSector = 0;
					currentDir.clear();
					currentDir.push(currentSector);
					path.clear();
					path.push("/");
				}
				else {
					if(nArr[0].equals("..")) {
						if(path.size() > 1 && currentDir.size() > 1) {
							path.pop();
							currentDir.pop();
							currentSector = currentDir.peek();
						}
					}
					else if(nArr[0].equals("")) { // absolute path
						Stack<String> pathBackup;
						Stack<Integer> currentDirBackup;
						currentSector = 0;
						currentDirBackup = (Stack<Integer>) currentDir.clone();
						currentDir.clear();
						currentDir.push(currentSector);
						pathBackup = (Stack<String>) path.clone();
						path.clear();
						path.push("/");
						for(int j = 1; j < nArr.length; j++) {
							fileEntry = searchName(nArr[j]);
							if(fileEntry != -1) {
								if(disk[currentSector][typePos * fileEntry] == 'D') {
									char[] arr = new char[4];
									for(int i = 0; i < 4; i++) {
										arr[i] = disk[currentSector][dirPos * fileEntry + linkOffset + i];
									}
									path.push(nArr[j]);
									currentSector = charArrToInt(arr);
									currentDir.push(currentSector);
								}
								else { // name is not a directory
									if(verbose == 1) {
										//System.out.println("file is not a directory.");
									}
									path = pathBackup;
									currentDir = currentDirBackup;
									currentSector = currentDir.peek();
									return -1;
								}
							}
							else { // name does not exist
								if(verbose == 1) {
									//System.out.println("file does not exist.");
								}
								path = pathBackup;
								currentDir = currentDirBackup;
								currentSector = currentDir.peek();
								return -1;
							}	
						}
						return 1;
					}
					else {	// relative path
						int count = 0;
						for(int j = 0; j < nArr.length; j++) {
							fileEntry = searchName(nArr[j]);
							if(fileEntry != -1) {
								if(disk[currentSector][typePos * fileEntry] == 'D') {
									char[] arr = new char[4];
									for(int i = 0; i < 4; i++) {
										arr[i] = disk[currentSector][dirPos * fileEntry + linkOffset + i];
									}
									path.push(nArr[j]);
									count++;
									currentSector = charArrToInt(arr);
									currentDir.push(currentSector);
								}
								else { // name is not a directory
									if(verbose == 1) {
										//System.out.println("file is not a directory.");
									}
									for(; count > 0; count--) {
										path.pop();
										currentDir.pop();
									}
									currentSector = currentDir.peek();
									return -1;
								}
							}
							else { // name does not exist
								if(verbose == 1) {
									//System.out.println("file does not exist.");
								}
								for(; count > 0; count--) {
									path.pop();
									currentDir.pop();
								}
								currentSector = currentDir.peek();
								return -1;
							}	
						}
						return 1;
					}
				}
			}
		}				
		else {
			if(verbose == 1) {
				System.out.println("A file is still open, couldn't change directory.");
			}
		}
		return 0;
	}
	
	void ls() {	
		Iterator<String> iter = path.iterator();
		System.out.print("File list under current directory [");
		if(iter.hasNext()) {
			System.out.print(iter.next());
		}
		while (iter.hasNext()){
		    System.out.print(iter.next());
		    System.out.print("/");
		}
		System.out.println("]");
		char[] arr2 = new char[2];
		char[] arr = new char[4];
		int frwdSector;
		int currSector = currentDir.peek();
		int cSector;
		int fSector;
		int count;
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currSector][typePos * i] == 'D' || disk[currSector][typePos * i] == 'U') {
					System.out.print(" Type: " + disk[currSector][typePos * i]);
					System.out.print(" Name: ");
					for(int j = 0; j < 9; j++) {
						System.out.print(disk[currSector][dirPos * i + nameOffset + j]);
					}
					for(int j = 0; j < 2; j++) {
						arr2[j] = disk[currSector][dirPos * i + sizeOffset + j];
					}
					count = 0;
					if(disk[currSector][typePos * i] == 'U') {
						for(int j = 0; j < 4; j++) {
							arr[j] = disk[currSector][dirPos * i + linkOffset + j];
						}
						cSector = charArrToInt(arr);
						do {
							for(int k = 0; k < 4; k++) {
								arr[k] = disk[cSector][frwdPos + k];
							}
							fSector = charArrToInt(arr);
							if(fSector != 0) {
								cSector = fSector;
								count++;
							}			
						} while(fSector != 0);
					}
					System.out.print(" Size: " + (charArrToInt(arr2) + count * 504));
					System.out.println("");
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
			}			
		} while(frwdSector != 0);
			
		numOfDirSectors = 0;
		numOfDataSectors = 0;	
		garbageCollection();
		int numOfFreeSector = 0;
		for(int i = 1; i < freeSectorList.length; i++) {
			if(freeSectorList[i] == 0) {
				numOfFreeSector++;
			}
		}
		System.out.println("Number of sectors: Free " + numOfFreeSector + "; Dir " + numOfDirSectors + "; Data " + numOfDataSectors);
	}
	
	void lsAll() {
		if(openMode == 'X') {
			cd("/");
			lsAllHelper();
			numOfDirSectors = 0;
			numOfDataSectors = 0;	
			garbageCollection();
			int numOfFreeSector = 0;
			for(int i = 1; i < freeSectorList.length; i++) {
				if(freeSectorList[i] == 0) {
					numOfFreeSector++;
				}
			}
			System.out.println("Number of sectors: Free " + numOfFreeSector + "; Dir " + numOfDirSectors + "; Data " + numOfDataSectors);
		}
		else {
			if(verbose == 1) {
				System.out.println("A file is still open, couldn't ls all.");
			}
		}
	}
	
	void lsAllHelper() {
		char[] arr2 = new char[2];
		char[] arr = new char[4];
		int frwdSector;
		int currSector = currentDir.peek();
		int cSector;
		int fSector;
		int count;
		String name = "";
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currSector][typePos * i] == 'D' || disk[currSector][typePos * i] == 'U') {
					System.out.print(" Type: " + disk[currSector][typePos * i]);
					System.out.print(" Full name: ");
					for(int j = 0; j < 9; j++) {
						name += disk[currSector][dirPos * i + nameOffset + j];
					}
					Iterator<String> iter = path.iterator();
					if(iter.hasNext()) {
						System.out.print(iter.next().replaceAll("\\s",""));
					}
					while (iter.hasNext()){
					    System.out.print(iter.next().replaceAll("\\s",""));
					    System.out.print("/");
					}
					for(int j = 0; j < 2; j++) {
						arr2[j] = disk[currSector][dirPos * i + sizeOffset + j];
					}
					count = 0;
					if(disk[currSector][typePos * i] == 'U') {
						for(int j = 0; j < 4; j++) {
							arr[j] = disk[currSector][dirPos * i + linkOffset + j];
						}
						cSector = charArrToInt(arr);
						do {
							for(int k = 0; k < 4; k++) {
								arr[k] = disk[cSector][frwdPos + k];
							}
							fSector = charArrToInt(arr);
							if(fSector != 0) {
								cSector = fSector;
								count++;
							}			
						} while(fSector != 0);
						System.out.print(name.replaceAll("\\s",""));
						System.out.print(" Size: " + (charArrToInt(arr2) + count * 504));
						System.out.println("");
					}
					else {
						System.out.print(name.replaceAll("\\s",""));
						System.out.print(" Size: " + (charArrToInt(arr2) + count * 504));
						System.out.println("");
						cd(name.replaceAll("\\s",""));
						lsAllHelper();
					}

				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
			}			
		} while(frwdSector != 0);
	}
	
	void garbageCollection() {
		freeSectorList = new int[totalSectors];
		freeSectorList[0] = 1;
		numOfDirSectors++;
		int currSector = 0;
		char[] arr = new char[4];
		int frwdSector;
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currSector][typePos * i] == 'D' || disk[currSector][typePos * i] == 'U') {
					for(int j = 0; j < 4; j++) {
						arr[j] = disk[currSector][dirPos * i + linkOffset + j];
					}
					int sector = charArrToInt(arr);
					freeSectorList[sector] = 1;
					if(disk[currSector][typePos * i] == 'D') {
						gcHelperForDir(sector);
						numOfDirSectors++;
					}
					else {
						gcHelperForFile(sector);
						numOfDataSectors++;
					}
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
				freeSectorList[currSector] = 1;
				numOfDirSectors++;
			}
		} while(frwdSector != 0);
	}
	
	void gcHelperForFile(int currSector) {
		char[] arr = new char[4];
		int frwdSector;
		do {	
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
				freeSectorList[currSector] = 1;
				numOfDataSectors++;
			}			
		} while(frwdSector != 0);
	}
	
	void gcHelperForDir(int currSector) {
		char[] arr = new char[4];
		int frwdSector;
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currSector][typePos * i] == 'D' || disk[currSector][typePos * i] == 'U') {
					for(int j = 0; j < 4; j++) {
						arr[j] = disk[currSector][dirPos * i + linkOffset + j];
					}
					int sector = charArrToInt(arr);
					freeSectorList[sector] = 1;
					if(disk[currSector][typePos * i] == 'D') {
						gcHelperForDir(sector);
						numOfDirSectors++;
					}
					else {
						gcHelperForFile(sector);
						numOfDataSectors++;
					}		
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
				freeSectorList[currSector] = 1;
				numOfDirSectors++;
			}			
		} while(frwdSector != 0);
	}
	
	int searchName(String name) {
		int currSector = currentSector;
		char[] arr = new char[4];
		int backSector;
		do {
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][backPos + i];
			}
			backSector = charArrToInt(arr);
			if(backSector != 0) {
				currSector = backSector;
			}
		} while(backSector != 0);		
		int frwdSector;
		do {
			for(int i = 1; i <= 31; i++) {
				if((disk[currSector][typePos * i] == 'D' || disk[currSector][typePos * i] == 'U') && compareName(currSector, i, name)) {
					currentSector = currSector;
					return i;
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currSector = frwdSector;
			}			
		} while(frwdSector != 0);
		return -1;
	}
	
	boolean compareName(int currSector, int i, String name) {
		char[] n = String.format("%-9s", name).toCharArray();
		for(int j = 0; j < 9; j++) {
			if(disk[currSector][dirPos * i + nameOffset + j] != n[j]) {
				return false;
			}
		}
		return true;
	}
	
	int firstFreeSector() {
		for(int i = 1; i < freeSectorList.length; i++) {
			if(freeSectorList[i] == 0) {
				char[] arr = new char[4];
				arr = intToCharArr(0, 4);
				for(int j = 0; j < 4; j++) {
					disk[i][backPos + j] = arr[j];
					disk[i][frwdPos + j] = arr[j];
				}
				freeSectorList[i] = 1;
				return i;
			}
		}
		System.out.println("Disk is full!");
		return -1;
	}
	
	int firstFreeDirEntry() {
		char[] arr = new char[4];
		int frwdSector;
		do {
			for(int i = 1; i <= 31; i++) {
				if(disk[currentSector][typePos * i] != 'D' && disk[currentSector][typePos * i] != 'U') {
					return i;
				}
			}
			for(int i = 0; i < 4; i++) {
				arr[i] = disk[currentSector][frwdPos + i];
			}
			frwdSector = charArrToInt(arr);
			if(frwdSector != 0) {
				currentSector = frwdSector;
			}					
		} while(frwdSector != 0);
		// expend the directory sector by linking a new sector to it
		int backSector = currentSector;
		frwdSector = firstFreeSector();
		if(frwdSector != -1) {
			arr = intToCharArr(frwdSector, 4);
			for(int i = 0; i < 4; i++) {
				disk[currentSector][frwdPos + i] = arr[i];
			}
			arr = intToCharArr(backSector, 4);
			currentSector = frwdSector;
			for(int i = 0; i < 4; i++) {
				disk[currentSector][frwdPos + i] = arr[i];
			}
			return 1;
		}
		return -1;
	}

	int charArrToInt(char[] arr) {
		StringBuilder sb = new StringBuilder();		
		for(char c : arr) {
			sb.append(String.format("%8s", Integer.toBinaryString((int)c)).replace(' ', '0'));
		}
		String binaryString = sb.toString();
		return Integer.parseUnsignedInt(binaryString, 2);
	}
	
	char[] intToCharArr(int freeSector, int arrLength) {
		char[] arr = new char[arrLength];
		String binaryString = String.format("%" + arrLength * 8 + "s", Integer.toBinaryString(freeSector)).replace(' ', '0');
		String[] byteStrings = binaryString.split("(?<=\\G.{" + 8 + "})");
		for(int i = 0; i < byteStrings.length; i++) {
			arr[i] = (char)Integer.parseInt(byteStrings[i], 2);	
		}
		return arr;
	}
	
	void saveDiskImage() throws IOException {
		FileWriter outputStream = null;
		try {
            outputStream = new FileWriter("diskImage.txt");
            for(int i = 0; i < totalSectors; i++) {
            	for(int j = 0; j < 512; j++) {
            		outputStream.write(disk[i][j]);
            	}
            	if(verbose == 1) {
            		outputStream.write('\n');
            	}
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
	}
	
	void loadDiskImage() throws IOException {
		FileReader inputStream = null;
		try {
            inputStream = new FileReader("diskImage.txt");
            for(int i = 0; i < totalSectors; i++) {
            	for(int j = 0; j < 512; j++) {
            		disk[i][j] = (char) inputStream.read();
            	}
            	if(verbose == 1) {
            		inputStream.read();
            	}
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
		garbageCollection();
	}
}
