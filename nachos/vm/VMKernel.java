package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		int numPhysPages = Machine.processor().getNumPhysPages();
		coreMap = new pageFrame[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			coreMap[i] = new pageFrame();

		// initialize swap file and memory lock
		swap = new SwapFile("nachos.swp");
		memoryLock = new Lock();
		allMemPinned = new Condition(memoryLock);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		//super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
		// delete swapfile here
	}

	/*
	 * Uses the simpliest replacement policy FIFO to decide what to evict next
	 * from the TLB.
	 * 
	 * return an index of the tlb that may be overwritten by
	 * machine.process().writeTLBentry( i, t );
	 */
	public static int replacementPolicy() {
		TranslationEntry emptyT = new TranslationEntry(0, 0, true, false,
				false, false);
		// pick a tlb entry to evict and replace it with a new
		// TranslationEntry();
		Machine.processor().writeTLBEntry(evictionIndex, emptyT);
		int TranslationEntryYouMayOverWrite = evictionIndex;//
		evictionIndex = (evictionIndex + 1)
				% (Machine.processor().getTLBSize());

		return TranslationEntryYouMayOverWrite;
	}

	// clock to find page to evict from phys memory
	// return the page to evict
	public pageFrame pageToEvict() {
		memoryLock.acquire();
		Integer numPinned = 0;
		Integer coreSize = Machine.processor().getNumPhysPages();

		// if all phys memory pages are pinned we must sleep the process
		// (allMemPinned.sleep())
		for (Integer i = 0; i < coreSize; i++)
			if (coreMap[i].pinCount != 0)
				numPinned++;

		if (numPinned == coreSize)
			allMemPinned.sleep();

		// we must sync the TLB first before we start changing phys Mem
		syncTLB(false);

		int coreIndex = 0;
		boolean pageFound = false;

		while (pageFound == false) {
			coreIndex = (coreIndex + 1) % coreSize;
			pageFrame page = coreMap[coreIndex];

			// dont look at pinned pages
			if (page.pinCount > 0)
				continue;

			// we want to evict an invalid entry or a page that doesn't have a
			// process associated to it
			if (page.te.valid == false || page.process == null)
				pageFound = true;

			// cant use a page recently used so set its used value to false once
			// we see it
			if (page.te.used == true)
				page.te.used = false;

			// else the clock has gone around the whole time coreMap and we just
			// want to evict the current page
			else
				pageFound = true;
		}

		// we should pin this page now
		pageFrame page = coreMap[coreIndex];
		page.pinCount = 1;
		page.freeWhenUnpinned = false;

		// now we have to tell the TLB that this entry should invalid
		for (Integer i = 0; i < Machine.processor().getTLBSize(); i++) {
			TranslationEntry TLBEntry = Machine.processor().readTLBEntry(i);
			if (TLBEntry.ppn == page.te.ppn) {
				TLBEntry.valid = false;
				Machine.processor().writeTLBEntry(i, TLBEntry);
				break;
			}
		}

		page.te.vpn = swap.insertPageIntoFile(page.te.ppn);

		memoryLock.release();
		return page;
	}
	
//	public static void handlePageFault( VMProcess process, int vpn )
//	{
//		//access coff file and get
//		//process.
//		
//		
//	}

	/*
	 * return a Translation entry from the core map based on the ppn
	 */
	public static TranslationEntry getTranslation(int ppn) {
		return coreMap[ppn].te;
	}
	
	public void handlePageFault( OpenFile coff, TranslationEntry pageTableEntry, boolean dirty, int vpn )
	{
		Integer pageSize = Processor.pageSize;
		//if the dirty bit is false we want to read from the coff file and put its information into
		//mainMemory
		if( dirty == false )
		{	
			pageFrame pageToEvict = pageToEvict();
			Integer numBits = coff.read( pageTableEntry.vpn * pageSize, mainMemory, 
						pageToEvict.te.ppn * pageSize, pageSize );
			Lib.assertTrue( numBits == pageSize );
		}
		
		//if the diry bit is true we want to read from the swap file and put its information into 
		//mainMemory
		else if( dirty == true )
		{
			pageFrame pageToEvict = pageToEvict();
			Integer numBits = swap.swapf.read( vpn * pageSize, mainMemory, 
					pageToEvict.te.ppn * pageSize, pageSize );
			Lib.assertTrue( numBits == pageSize );
		}
	}

	/* Translate a vpn to a ppn */
	public static int translatePage(VMProcess process, int vpn) {
		Lib.assertTrue(memoryLock.isHeldByCurrentThread());
		Integer ppn = -1;
		// try to find the translation entry for this vpn in physical memory
		for (int i = 0; i < Machine.processor().getNumPhysPages(); i++) {
			if ( coreMap[i].te.vpn == vpn && coreMap[i].process == process )
				return coreMap[i].te.ppn;
		}
		
//		handlePageFault();
//		for( int i = 0; i <  Machine.processor().getTLBSize(); i++ )
//		{
//			TranslationEntry TLBEntry = Machine.processor().readTLBEntry(i);
//			if( TLBEntry.vpn == vpn ) return TLBEntry.ppn;
//		}
		
		
		//if the page doesnt exist in physical memory then its either in the coff file or its
		// in the swap file, so we would have a page fault here then
		
		// if it isnt in physical memory you may have to find it in the swap
		// file
		// or coff file

		// JUST RETURN -1 FOR NOW IF ENTRY ISNT IN MEMORY
		return ppn;

		// find PageFrame that matches process and vpn
		// if found, return ppn
		// else, fetch page

		// return 0;// temporarily return 0 until this is implemented
	}

	public static void syncTLB(boolean contextSwitch) {
		// iterate through the entire tlb and start syncing
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			// get the TableEntry stored at the ith tlb location
			TranslationEntry TLBEntry = Machine.processor().readTLBEntry(i);

			// If this TLB Entry is valid we must sync it up with the
			// corresponding Table Entry stored in physical memory
			if (TLBEntry.valid) {
				TranslationEntry coreEntry = coreMap[TLBEntry.ppn].te;
				// not sure if this is right cause dorian said:
				// "sync tlb settings (used/dirty) -- e.g., if either
				// is true, set true"
				coreEntry.dirty |= TLBEntry.dirty;
				coreEntry.used |= TLBEntry.used;

				// we must set valid bit to false if contextSwitch is
				// true
				if (contextSwitch)
					TLBEntry.valid = false;

				// if contextSwitch is false then just set used/dirty
				// to false to make that
				// table entry available to read and write to
				else {
					TLBEntry.dirty = false;
					TLBEntry.used = false;
				}

				// make sure to put/save changes back into TLB
				Machine.processor().writeTLBEntry(i, TLBEntry);
			}
		}
	}

	/*
	 * Hints from DORIAN ================= pages are pinned by
	 * read/writeVirtualMemory() functions. They are also pinned when fetching a
	 * new page (reading from COFF or swap) or cleaning a page (writing to
	 * swap).
	 */
	public static class pageFrame {
		public VMProcess process; // valid if entry.valid
		public TranslationEntry te = new TranslationEntry();
		public int pinCount; // valid if te.valid
		public boolean freeWhenUnpinned; // valid if pinned
	}

	// Make a swapFile class to make it easier to create a swapFile and access
	// it
	private class SwapFile {
		private OpenFile swapf = null;
		// private LinkedList<Integer> pageTableIDs = new LinkedList<Integer>();
		private LinkedList<Integer> unusedFileSpace = new LinkedList<Integer>();
		//private LinkedList<Integer> usedFileSpace = new LinkedList<Integer>();

		public SwapFile(String filename) {
			swapf = ThreadedKernel.fileSystem.open(filename, true);

		}

		// insert a page table into the swap file
		// param @ ppn is the physical page number of the table entry you want
		// to insert into the file
		// return the spn so we can set the vpn of the calling TE to spn
		public Integer insertPageIntoFile(Integer ppn) {
			int spn = 0;

			// if first element to placed in swap file add 0 to usedFilespace
			//if (unusedFileSpace == null && usedFileSpace == null)
				//usedFileSpace.addLast(spn);

			// if there is anything in unusedFileSpace we want that to be our
			// spn
			if (unusedFileSpace != null)
				spn = unusedFileSpace.pop();

			// if unusedFilespace is empty we want the last element of the used
			// file
			// space and use that number +1, then put that number in the used
			// list
			else {
				spn = usedFileSpace.getLast() + 1;
				usedFileSpace.addLast(spn);
			}

			int ps = Machine.processor().pageSize;

			// write into file swapf starting at position swapFileNumber *
			// pageSize
			// write from buffer mainMemory starting at position
			// physicalPageNumber * PageSize
			int numBits = swapf.write(spn * ps, mainMemory, ppn * ps, ps);

			// make sure there was no error writing memory to the file and that
			// the ammount of
			// bits told to write in were written in
			Lib.assertTrue(numBits != -1 || numBits == ppn * ps);

			// set the vpn in the te for this to be equal to the spn
			return spn;

			// if there is a failure here you might want to exit

			// We might want to evict a page from coreMap here

			// PageTableIDs.push(spn);
		}

		// This Function is not nearly done
		public void extractPageFromFile(int vpn) {

			usedFileSpace.addLast(vpn);
		}
	}

	public SwapFile swap = null;
	public static Lock memoryLock;// = new Lock();
	public static Condition allMemPinned = null;

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	protected static byte[] mainMemory = Machine.processor().getMemory();

	public static pageFrame[] coreMap = null;
	private static final char dbgVM = 'v';
	private static int evictionIndex = 0;
}
