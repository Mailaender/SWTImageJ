package ij.text;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PlotContentsDialog;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.measure.ResultsTableMacros;
import ij.plugin.Distribution;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.Recorder;
import ij.util.Tools;

/**
 * This is an unlimited size text panel with tab-delimited, labeled and
 * resizable columns. It is based on the hGrid class at
 * http://www.lynx.ch/contacts/~/thomasm/Grid/index.html.
 */
public class TextPanel implements AdjustmentListener, org.eclipse.swt.events.MouseListener, org.eclipse.swt.events.MouseMoveListener, org.eclipse.swt.events.MouseTrackListener, org.eclipse.swt.events.KeyListener, ClipboardOwner, org.eclipse.swt.events.SelectionListener, org.eclipse.swt.events.MouseWheelListener, Runnable {

	static final int DOUBLE_CLICK_THRESHOLD = 650;
	// height / width
	int iGridWidth, iGridHeight;
	int iX, iY;
	// data
	String[] sColHead;
	Vector vData;
	int[] iColWidth;
	int iColCount, iRowCount;
	int iRowHeight, iFirstRow;
	Slider sbHoriz;
	// scrolling
	Slider sbVert;
	int iSbWidth, iSbHeight;
	boolean bDrag;
	int iXDrag, iColDrag;
	boolean headings = true;
	String title = "";
	String labels;
	org.eclipse.swt.events.KeyListener keyListener;
	// Cursor resizeCursor = new Cursor(Cursor.E_RESIZE_CURSOR);
	// Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	protected org.eclipse.swt.graphics.Cursor defaultCursor = new org.eclipse.swt.graphics.Cursor(Display.getDefault(), SWT.CURSOR_ARROW);
	protected org.eclipse.swt.graphics.Cursor resizeCursor = new org.eclipse.swt.graphics.Cursor(Display.getDefault(), SWT.CURSOR_SIZESE);
	int selStart = -1, selEnd = -1, selOrigin = -1, selLine = -1;
	TextCanvas tc;
	org.eclipse.swt.widgets.Menu pm;
	boolean columnsManuallyAdjusted;
	long mouseDownTime;
	String filePath;
	ResultsTable rt;
	boolean unsavedLines;
	String searchString = "";
	org.eclipse.swt.widgets.Menu fileMenu, editMenu;
	boolean menusExtended;
	boolean saveAsCSV;
	private Shell shell;
	private boolean drag;
	private ScrolledComposite scrollCanvas;
	private TextWindow textWindow;
	protected boolean isVisible;

	public TextCanvas getTc() {

		return tc;
	}

	public Shell getShell() {

		return shell;
	}

	public boolean isVisible() {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if(shell.isDisposed()) {
					isVisible = false;
					return;
				}
				isVisible = shell.isVisible();
			}
		});
		return isVisible;
	}

	/** Constructs a new TextPanel. */
	public TextPanel(TextWindow textWindow, Shell parent) {
		// scrollCanvas = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		// scrollCanvas.setLayoutData(new FillLayout());
		// if set the scrollbars dont appear but the composite expand on the whole view
		// scrollCanvas.setExpandHorizontal(true);
		// scrollCanvas.setExpandVertical(true);

		// Color white = new org.eclipse.swt.graphics.Color(Display.getDefault(), 255,
		// 255, 255);
		// scrolledWrapper.setBackground(Display.getCurrent().getSystemColor(BG_COLOR));
		this.shell = parent;
		this.textWindow = textWindow;
		tc = new TextCanvas(this);
		tc.setLayoutData(ij.layout.BorderLayout.CENTER);
		// scrollCanvas.setContent(tc);
		// scrollCanvas.setMinSize(tc.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		// add("Center",tc);
		// sbHoriz=new Scrollbar(Scrollbar.HORIZONTAL);
		// sbHoriz = scrollCanvas.getVerticalBar();
		// GUI.fixScrollbar(sbHoriz);
		sbHoriz = new Slider(parent, SWT.HORIZONTAL);
		sbHoriz.setSelection(0);
		sbHoriz.setThumb(1);
		sbHoriz.setIncrement(1);
		sbHoriz.setPageIncrement(1);
		sbHoriz.addSelectionListener(this);
		sbHoriz.setLayoutData(ij.layout.BorderLayout.SOUTH);
		// sbHoriz.setFocusable(false); // prevents scroll bar from blinking on Windows
		// add("South", sbHoriz);
		sbVert = new Slider(parent, SWT.VERTICAL);
		sbVert.setThumb(1);
		sbVert.setIncrement(1);
		sbVert.setPageIncrement(1);
		sbVert.setLayoutData(ij.layout.BorderLayout.EAST);
		// GUI.fixScrollbar(sbVert);
		sbVert.addSelectionListener(this);
		// sbVert.setFocusable(false);
		ImageJ ij = IJ.getInstance();
		if(ij != null) {
			// Changed for SWT To do!
			sbHoriz.addKeyListener(ij);
			sbVert.addKeyListener(ij);
		}
		// add("East", sbVert);
		addPopupMenu();
	}

	/** Constructs a new TextPanel. */
	public TextPanel(TextWindow textWindow, Shell parent, String title) {

		this(textWindow, parent);
		this.textWindow = textWindow;
		this.title = title;
		if(title.equals("Results") || title.endsWith("(Results)")) {
			new org.eclipse.swt.widgets.MenuItem(pm, SWT.SEPARATOR);
			addPopupItem("Clear Results");
			addPopupItem("Summarize");
			addPopupItem("Distribution...");
			addPopupItem("Set Measurements...");
		}
	}

	void addPopupMenu() {

		pm = new org.eclipse.swt.widgets.Menu(shell, SWT.POP_UP);
		addPopupItem("Save As...");
		addPopupItem("Table Action");
		new org.eclipse.swt.widgets.MenuItem(pm, SWT.SEPARATOR);
		addPopupItem("Cut");
		addPopupItem("Copy");
		addPopupItem("Clear");
		addPopupItem("Select All");
		// add(pm);
	}

	void addPopupItem(String s) {

		org.eclipse.swt.widgets.MenuItem mi = new org.eclipse.swt.widgets.MenuItem(pm, SWT.PUSH);
		mi.setText(s);
		mi.addSelectionListener(this);
		// pm.add(mi);
	}

	/**
	 * Clears this TextPanel and sets the column headings to those in the
	 * tab-delimited 'headings' String. Set 'headings' to "" to use a single column
	 * with no headings.
	 */
	public synchronized void setColumnHeadings(String labels) {

		// if (count++==5) throw new IllegalArgumentException();
		boolean sameLabels = labels.equals(this.labels);
		this.labels = labels;
		if(labels.equals("")) {
			iColCount = 1;
			sColHead = new String[1];
			sColHead[0] = "";
		} else {
			if(labels.endsWith("\t"))
				this.labels = labels.substring(0, labels.length() - 1);
			sColHead = this.labels.split("\t");
			iColCount = sColHead.length;
		}
		flush();
		vData = new Vector();
		if(!(iColWidth != null && iColWidth.length == iColCount && sameLabels && iColCount != 1)) {
			iColWidth = new int[iColCount];
			columnsManuallyAdjusted = false;
		}
		iRowCount = 0;
		resetSelection();
		adjustHScroll();
		tc.repaint();
	}

	/** Returns the column headings as a tab-delimited string. */
	public String getColumnHeadings() {

		return labels == null ? "" : labels;
	}

	public synchronized void updateColumnHeadings(String labels) {

		this.labels = labels;
		if(labels.equals("")) {
			iColCount = 1;
			sColHead = new String[1];
			sColHead[0] = "";
		} else {
			if(labels.endsWith("\t"))
				this.labels = labels.substring(0, labels.length() - 1);
			sColHead = this.labels.split("\t");
			iColCount = sColHead.length;
			iColWidth = new int[iColCount];
			columnsManuallyAdjusted = false;
		}
	}

	public void setFont(org.eclipse.swt.graphics.Font font, boolean antialiased) {

		tc.fFont = font;
		tc.iImage = null;
		tc.fMetrics = null;
		tc.antialiased = antialiased;
		iColWidth[0] = 0;
		if(isVisible())
			updateDisplay();
	}

	/** Adds a single line to the end of this TextPanel. */
	public void appendLine(String text) {

		if(vData == null)
			setColumnHeadings("");
		char[] chars = text.toCharArray();
		vData.addElement(chars);
		iRowCount++;
		if(isVisible()) {
			if(iColCount == 1 && tc.fMetrics != null) {
				iColWidth[0] = (int)Math.max(iColWidth[0], tc.fMetrics.getAverageCharacterWidth());
				adjustHScroll();
			}
			updateDisplay();
			unsavedLines = true;
		}
	}

	/** Adds one or more lines to the end of this TextPanel. */
	public void append(String text) {

		if(text == null)
			text = "null";
		if(vData == null)
			setColumnHeadings("");
		if(text.length() == 1 && text.equals("\n"))
			text = "";
		String[] lines = text.split("\n");
		for(int i = 0; i < lines.length; i++)
			appendWithoutUpdate(lines[i]);
		if(isVisible()) {
			updateDisplay();
			unsavedLines = true;
		}
	}

	/** Adds strings contained in an ArrayList to the end of this TextPanel. */
	public void append(ArrayList list) {

		if(list == null)
			return;
		if(vData == null)
			setColumnHeadings("");
		for(int i = 0; i < list.size(); i++)
			appendWithoutUpdate((String)list.get(i));
		if(isVisible()) {
			updateDisplay();
			unsavedLines = true;
		}
	}

	/**
	 * Adds a single line to the end of this TextPanel without updating the display.
	 */
	public void appendWithoutUpdate(String data) {

		if(vData != null) {
			char[] chars = data.toCharArray();
			vData.addElement(chars);
			iRowCount++;
		}
	}

	public void updateDisplay() {

		iY = iRowHeight * (iRowCount + 1);
		adjustVScroll();
		if(iColCount > 1 && iRowCount <= 10 && !columnsManuallyAdjusted)
			iColWidth[0] = 0; // forces column width calculation
		tc.repaint();
	}

	String getCell(int column, int row) {

		if(column < 0 || column >= iColCount || row < 0 || row >= iRowCount)
			return null;
		return new String(getChars(column, row));
	}

	synchronized char[] getChars(int column, int row) {

		if(vData == null)
			return null;
		if(row >= vData.size())
			return null;
		char[] chars = row >= 0 && row < vData.size() ? (char[])(vData.elementAt(row)) : null;
		if(chars == null || chars.length == 0)
			return null;
		if(iColCount == 1)
			return chars;
		int start = 0;
		int tabs = 0;
		int length = chars.length;
		while(column > tabs) {
			if(chars[start] == '\t')
				tabs++;
			start++;
			if(start >= length)
				return null;
		}
		;
		if(start < 0 || start >= chars.length) {
			System.out.println("start=" + start + ", chars.length=" + chars.length);
			return null;
		}
		if(chars[start] == '\t')
			return null;
		int end = start;
		while(chars[end] != '\t' && end < (length - 1))
			end++;
		if(chars[end] == '\t')
			end--;
		char[] chars2 = new char[end - start + 1];
		for(int i = 0, j = start; i < chars2.length; i++, j++) {
			chars2[i] = chars[j];
		}
		return chars2;
	}

	/* Changed for SWT. Deleted synchronized keyword! */
	public void adjustVScroll() {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if(iRowHeight == 0)
					return;
				Point d = tc.getSize();
				int value = iY / iRowHeight;
				int visible = d.y / iRowHeight;
				int maximum = iRowCount + 1;
				if(visible < 0)
					visible = 0;
				if(visible > maximum)
					visible = maximum;
				if(value > (maximum - visible))
					value = maximum - visible;
				// sbVert.setValues(value,visible,0,maximum);
				sbVert.setSelection(value);
				sbVert.setThumb(visible);
				sbVert.setMaximum(maximum);
				sbVert.setMinimum(0);
				iY = iRowHeight * value;
			}
		});
	}

	/* Changed for SWT. Deleted synchronized keyword! */
	public void adjustHScroll() {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if(iRowHeight == 0)
					return;
				Point d = tc.getSize();
				int w = 0;
				for(int i = 0; i < iColCount; i++)
					w += iColWidth[i];
				iGridWidth = w;
				// sbHoriz.setValues(iX,d.width,0,iGridWidth);
				sbHoriz.setSelection(iX);
				sbHoriz.setThumb(d.x);
				sbHoriz.setMinimum(0);
				sbHoriz.setMaximum(iGridWidth);
				iX = sbHoriz.getSelection();
			}
		});
	}

	public void adjustmentValueChanged(SelectionEvent e) {

		iX = sbHoriz.getSelection();
		iY = iRowHeight * sbVert.getSelection();
		tc.redraw();
	}

	private void showLinePos() { // show line numbers in status bar (Norbert Visher)

		int startLine = getSelectionStart() + 1;
		int endLine = getSelectionEnd() + 1;
		String msg = "Line " + startLine;
		if(startLine != endLine) {
			msg += "-" + endLine;
		}
		if(!msg.equals("Line 0"))
			IJ.showStatus(msg);
	}

	public void mousePressed(org.eclipse.swt.events.MouseEvent e) {

		int x = e.x, y = e.y;
		if(e.button == 3 || e.stateMask == SWT.ALT || e.stateMask == SWT.CONTROL) {
			// pm.show(e.getComponent(),x,y);
			org.eclipse.swt.graphics.Point p = shell.toDisplay(e.x, e.y);
			pm.setLocation(p.x, p.y);
			pm.setVisible(true);
		} else if(e.stateMask == SWT.SHIFT)
			extendSelection(x, y);
		else {
			select(x, y);
		}
	}

	void handleDoubleClick() {// Marcel Boeglin 2019.10.07

		boolean overlayList = title.startsWith("Overlay Elements of ");
		if(selStart < 0 || selStart != selEnd || (iColCount != 1 && !overlayList))
			return;
		/* In SWT we have a double-click listener! */
		// boolean doubleClick = System.currentTimeMillis() - mouseDownTime <= DOUBLE_CLICK_THRESHOLD;
		mouseDownTime = System.currentTimeMillis();
		// if (doubleClick) {
		char[] chars = (char[])(vData.elementAt(selStart));
		String s = new String(chars);
		if(overlayList) {
			String owner = title.substring(20, title.length());
			String[] titles = WindowManager.getImageTitles();
			for(int i = 0; i < titles.length; i++) {
				String t = titles[i];
				if(titles[i].equals(owner)) {
					ImagePlus imp = WindowManager.getImage(owner);
					WindowManager.setTempCurrentImage(imp);// ?
					Shell frame = imp.getWindow().getShell();
					frame.forceActive();
					/*
					 * if (frame.getState() == Frame.ICONIFIED) frame.setState(Frame.NORMAL);
					 */
					handleDoubleClickInOverlayList(s);
					break;
				}
			}
			return;
		}
		int index = s.indexOf(": ");
		if(index > -1 && !s.endsWith(": "))
			s = s.substring(index + 2); // remove sequence number added by ListFilesRecursively
		if(s.indexOf(File.separator) != -1 || s.indexOf(".") != -1) {
			filePath = s;
			Thread thread = new Thread(this, "Open");
			thread.setPriority(thread.getPriority() - 1);
			thread.start();
		}
		// }
	}

	private void handleDoubleClickInOverlayList(String s) {// Marcel Boeglin 2019.10.09

		ImagePlus imp = WindowManager.getCurrentImage();
		if(imp == null)
			return;
		Overlay overlay = imp.getOverlay();
		if(overlay == null)
			return;
		String[] columns = s.split("\t");
		int index = (int)Tools.parseDouble(columns[1]);
		Roi roi = overlay.get(index);
		if(roi == null)
			return;
		if(imp.isHyperStack()) {
			int c = roi.getCPosition();
			int z = roi.getZPosition();
			int t = roi.getTPosition();
			c = c == 0 ? imp.getChannel() : c;
			z = z == 0 ? imp.getSlice() : z;
			t = t == 0 ? imp.getFrame() : t;
			imp.setPosition(c, z, t);
		} else {
			int p = roi.getPosition();
			if(p >= 1 && p <= imp.getStackSize())
				imp.setPosition(p);
		}
		imp.setRoi(roi);
	}

	/**
	 * For better performance, open double-clicked files on separate thread instead
	 * of on event dispatch thread.
	 */
	public void run() {

		if(filePath == null)
			return;
		File f = new File(filePath);
		if(f.exists() || filePath.startsWith("https"))
			IJ.open(filePath);
	}

	@Override
	public void mouseMove(org.eclipse.swt.events.MouseEvent e) {

		if(drag) {
			mouseDragged(e);
		}
	}

	@Override
	public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {

		handleDoubleClick();
	}

	@Override
	public void mouseDown(org.eclipse.swt.events.MouseEvent e) {

		mousePressed(e);
		drag = true;
	}

	@Override
	public void mouseUp(org.eclipse.swt.events.MouseEvent e) {

		mouseReleased(e);
		drag = false;
		// mouseClicked implementation!
		if(e.count == 2) {
			// e.consume();
			boolean doubleClickableTable = title != null && (title.equals("Log") || title.startsWith("Overlay Elements"));
			Hashtable commands = Menus.getCommands();
			boolean tableActionCommand = commands != null && commands.get("Table Action") != null;
			if(!tableActionCommand)
				tableActionCommand = ij.plugin.MacroInstaller.isMacroCommand("Table Action");
			if(doubleClickableTable || !tableActionCommand)
				return;
			String options = title + "|" + getSelectionStart() + "|" + getSelectionEnd();
			IJ.run("Table Action", options);
		}
	}

	@Override
	public void mouseScrolled(org.eclipse.swt.events.MouseEvent e) {

		mouseWheelMoved(e);
	}

	@Override
	public void mouseExit(MouseEvent arg0) {

		if(bDrag) {
			shell.setCursor(defaultCursor);
			bDrag = false;
		}
	}

	@Override
	public void mouseHover(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseMoved(org.eclipse.swt.events.MouseEvent e) {

		int x = e.x, y = e.y;
		if(y <= iRowHeight) {
			int xb = x;
			x = x + iX - iGridWidth;
			int i = iColCount - 1;
			for(; i >= 0; i--) {
				if(x > -7 && x < 7)
					break;
				x += iColWidth[i];
			}
			if(i >= 0) {
				if(!bDrag) {
					shell.setCursor(resizeCursor);
					bDrag = true;
					iXDrag = xb - iColWidth[i];
					iColDrag = i;
				}
				return;
			}
		}
		if(bDrag) {
			shell.setCursor(defaultCursor);
			bDrag = false;
		}
	}

	public void mouseDragged(org.eclipse.swt.events.MouseEvent e) {

		if(e.button == 3 || e.stateMask == SWT.ALT || e.stateMask == SWT.CONTROL)
			return;
		int x = e.x, y = e.y;
		if(bDrag && x < tc.getSize().x) {
			int w = x - iXDrag;
			if(w < 0)
				w = 0;
			iColWidth[iColDrag] = w;
			columnsManuallyAdjusted = true;
			adjustHScroll();
			tc.repaint();
		} else {
			extendSelection(x, y);
		}
	}

	public void mouseReleased(org.eclipse.swt.events.MouseEvent e) {

		showLinePos();
	}

	public void mouseWheelMoved(org.eclipse.swt.events.MouseEvent event) {

		synchronized(this) {
			int rot = event.count;
			sbVert.setSelection(sbVert.getSelection() + rot);
			iY = iRowHeight * sbVert.getSelection();
			tc.repaint();
		}
	}

	private void scroll(int inc) {

		synchronized(this) {
			sbVert.setSelection(sbVert.getSelection() + inc);
			iY = iRowHeight * sbVert.getSelection();
			tc.repaint();
		}
	}

	/** Unused keyPressed and keyTyped events will be passed to 'listener'. */
	public void addKeyListener(org.eclipse.swt.events.KeyListener listener) {

		keyListener = listener;
	}

	public void addMouseListener(org.eclipse.swt.events.MouseListener listener) {

		tc.addMouseListener(listener);
	}

	public void keyPressed(org.eclipse.swt.events.KeyEvent e) {

		int key = e.keyCode;
		char c = e.character;
		if(key == SWT.BS || key == SWT.DEL)
			clearSelection();
		else if(key == SWT.ARROW_UP)
			scroll(-1);
		else if(key == SWT.ARROW_DOWN)
			scroll(1);
		else if(keyListener != null && c != 's' && c != 'c' && c != 'x' && c != 'a' && c != 'f' && c != 'g')
			keyListener.keyPressed(e);
	}

	@Override
	public void keyReleased(org.eclipse.swt.events.KeyEvent e) {

		IJ.setKeyUp(e.keyCode);
		showLinePos();
	}

	public void keyTyped(org.eclipse.swt.events.KeyEvent e) {

		if(keyListener != null)
			keyListener.keyReleased(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void widgetSelected(SelectionEvent e) {

		if(e.widget instanceof Slider) {
			adjustmentValueChanged(e);
		} else {
			actionPerformed(e);
		}
	}

	public void actionPerformed(SelectionEvent e) {

		org.eclipse.swt.widgets.MenuItem m = (org.eclipse.swt.widgets.MenuItem)e.widget;
		String cmd = m.getText();
		doCommand(cmd);
	}

	void doCommand(String cmd) {

		if(cmd == null)
			return;
		if(cmd.equals("Save As..."))
			saveAs("");
		else if(cmd.equals("Cut"))
			cutSelection();
		else if(cmd.equals("Copy"))
			copySelection();
		else if(cmd.equals("Clear"))
			doClear();
		else if(cmd.equals("Select All"))
			selectAll();
		else if(cmd.equals("Find..."))
			find(null);
		else if(cmd.equals("Find Next"))
			find(searchString);
		else if(cmd.equals("Rename..."))
			rename(null);
		else if(cmd.equals("Duplicate..."))
			duplicate();
		else if(cmd.equals("Summarize")) {
			if("Results".equals(title))
				IJ.doCommand("Summarize");
			else {
				Analyzer analyzer = new Analyzer(null, getResultsTable());
				analyzer.summarize();
			}
		} else if(cmd.equals("Distribution...")) {
			if("Results".equals(title))
				IJ.doCommand("Distribution...");
			else
				new Distribution().run(getResultsTable());
		} else if(cmd.equals("Clear Results"))
			doClear();
		else if(cmd.equals("Set Measurements..."))
			IJ.doCommand("Set Measurements...");
		else if(cmd.equals("Options..."))
			IJ.doCommand("Input/Output...");
		else if(cmd.equals("Apply Macro..."))
			new ResultsTableMacros(rt);
		else if(cmd.equals("Sort..."))
			sort();
		else if(cmd.equals("Plot..."))
			// new PlotContentsDialog(title,
			// getOrCreateResultsTable()).showDialog(getParent() instanceof Shell ? (Shell)
			// getParent() : null);
			new PlotContentsDialog(title, getOrCreateResultsTable()).showDialog(shell);
		else if(cmd.equals("Table Action")) {
			String options = title + "|" + getSelectionStart() + "|" + getSelectionEnd();
			IJ.run("Table Action", options);
		}
	}

	public void lostOwnership(Clipboard clip, Transferable cont) {

	}

	private void find(String s) {

		int first = 0;
		if(s == null) {
			GenericDialog gd = new GenericDialog("Find...", getTextWindow().getShell());
			gd.addStringField("Find: ", searchString, 20);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			s = gd.getNextString();
		} else {
			if(selEnd >= 0 && selEnd < iRowCount - 1)
				first = selEnd + 1;
			else {
				IJ.beep();
				return;
			}
		}
		if(s.equals(""))
			return;
		boolean found = false;
		for(int i = first; i < iRowCount; i++) {
			String line = new String((char[])(vData.elementAt(i)));
			if(line.contains(s)) {
				setSelection(i, i);
				found = true;
				first = i + 1;
				break;
			}
		}
		if(!found) {
			IJ.beep();
			first = 0;
		}
		searchString = s;
	}

	public TextWindow getTextWindow() {

		// Component comp = getParent();
		if(textWindow == null)
			return null;
		else
			return textWindow;
	}

	void rename(String title2) {

		ResultsTable rt2 = getOrCreateResultsTable();
		if(rt2 == null)
			return;
		if(title2 != null && title2.equals(""))
			title2 = null;
		TextWindow tw = getTextWindow();
		if(tw == null)
			return;
		if(title2 == null) {
			GenericDialog gd = new GenericDialog("Rename", tw.getShell());
			gd.addStringField("Title:", getNewTitle(title), 20);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			title2 = gd.getNextString();
		}
		String title1 = title;
		if(title != null && title.equals("Results")) {
			IJ.setTextPanel(null);
			Analyzer.setUnsavedMeasurements(false);
			Analyzer.setResultsTable(null);
			Analyzer.resetCounter();
		}
		if(title2.equals("Results")) {
			// tw.setVisible(false);
			tw.getShell().close();
			WindowManager.removeWindow(tw);
			flush();
			rt2.show("Results");
		} else {
			tw.getShell().setText(title2);
			title = title2;
			rt2.show(title);
		}
		Menus.updateWindowMenuItem(title1, title2);
		if(IJ.recording()) {
			if(Recorder.scriptMode())
				Recorder.recordString("IJ.renameResults(\"" + title1 + "\", \"" + title2 + "\");\n");
			else
				Recorder.record("Table.rename", title1, title2);
		}
	}

	void duplicate() {

		ResultsTable rt2 = getOrCreateResultsTable();
		if(rt2 == null)
			return;
		rt2 = (ResultsTable)rt2.clone();
		String title2 = IJ.getString("Title:", getNewTitle(title));
		if(!title2.equals("")) {
			if(title2.equals("Results"))
				title2 = "Results2";
			rt2.show(title2);
		}
	}

	private String getNewTitle(String oldTitle) {

		if(oldTitle == null)
			return "Table2";
		String title2 = oldTitle;
		if(title2.endsWith("-1") || title2.endsWith("-2"))
			title2 = title2.substring(0, title.length() - 2);
		String title3 = title2 + "-1";
		if(title3.equals(oldTitle))
			title3 = title2 + "-2";
		return title3;
	}

	void select(int x, int y) {

		Point d = tc.getSize();
		if(iRowHeight == 0 || x > d.x || y > d.y)
			return;
		int r = (y / iRowHeight) - 1 + iFirstRow;
		int lineWidth = iGridWidth;
		if(iColCount == 1 && tc.fMetrics != null && r >= 0 && r < iRowCount) {
			char[] chars = (char[])vData.elementAt(r);
			lineWidth = (int)Math.max(tc.fMetrics.getAverageCharacterWidth() * chars.length, iGridWidth);
		}
		if(r >= 0 && r < iRowCount && x < lineWidth) {
			selOrigin = r;
			selStart = r;
			selEnd = r;
		} else {
			resetSelection();
			selOrigin = r;
			if(r >= iRowCount)
				selOrigin = iRowCount - 1;
		}
		tc.repaint();
		selLine = r;
		Interpreter interp = Interpreter.getInstance();
		if(interp != null && title.equals("Debug"))
			interp.showArrayInspector(r);
	}

	void extendSelection(int x, int y) {

		Point d = tc.getSize();
		if(iRowHeight == 0 || x > d.x || y > d.y)
			return;
		int r = (y / iRowHeight) - 1 + iFirstRow;
		if(r >= 0 && r < iRowCount) {
			if(r < selOrigin) {
				selStart = r;
				selEnd = selOrigin;
			} else {
				selStart = selOrigin;
				selEnd = r;
			}
		}
		tc.repaint();
		selLine = r;
	}

	/** Converts a y coordinate in pixels into a row index. */
	public int rowIndex(int y) {

		if(y > tc.getSize().y)
			return -1;
		else
			return (y / iRowHeight) - 1 + iFirstRow;
	}

	/**
	 * Copies the current selection to the system clipboard. Returns the number of
	 * characters copied.
	 */
	public int copySelection() {

		if(IJ.recording() && title.equals("Results"))
			Recorder.record("String.copyResults");
		if(selStart == -1 || selEnd == -1)
			return copyAll();
		StringBuffer sb = new StringBuffer();
		ResultsTable rt2 = getResultsTable();
		boolean hasRowNumers = rt2 != null && rt2.showRowNumbers();
		if(Prefs.copyColumnHeaders && labels != null && !labels.equals("") && selStart == 0 && selEnd == iRowCount - 1) {
			if(hasRowNumers && Prefs.noRowNumbers) {
				String s = labels;
				int index = s.indexOf("\t");
				if(index != -1)
					s = s.substring(index + 1, s.length());
				sb.append(s);
			} else
				sb.append(labels);
			sb.append('\n');
		}
		for(int i = selStart; i <= selEnd; i++) {
			char[] chars = (char[])(vData.elementAt(i));
			String s = new String(chars);
			if(s.endsWith("\t"))
				s = s.substring(0, s.length() - 1);
			if(hasRowNumers && Prefs.noRowNumbers && labels != null && !labels.equals("")) {
				int index = s.indexOf("\t");
				if(index != -1)
					s = s.substring(index + 1, s.length());
				sb.append(s);
			} else
				sb.append(s);
			if(i < selEnd || selEnd > selStart)
				sb.append('\n');
		}
		String s = new String(sb);
		/*
		 * Clipboard clip = getToolkit().getSystemClipboard(); if (clip==null) return 0;
		 * StringSelection cont = new StringSelection(s); clip.setContents(cont,this);
		 */
		org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(Display.getDefault());
		TextTransfer textTransfer = TextTransfer.getInstance();
		cb.setContents(new Object[]{s}, new Transfer[]{textTransfer});
		if(s.length() > 0) {
			IJ.showStatus((selEnd - selStart + 1) + " lines copied to clipboard");
			// if (this.getParent() instanceof ImageJ)
			if(shell instanceof Shell)
				Analyzer.setUnsavedMeasurements(false);
		}
		return s.length();
	}

	int copyAll() {

		selectAll();
		int count = selEnd - selStart + 1;
		if(count > 0)
			copySelection();
		resetSelection();
		unsavedLines = false;
		return count;
	}

	void cutSelection() {

		if(selStart == -1 || selEnd == -1)
			selectAll();
		copySelection();
		clearSelection();
	}

	/** Implements the Clear command. */
	public void doClear() {

		if(getLineCount() > 0 && selStart != -1 && selEnd != -1)
			clearSelection();
		else if("Results".equals(title))
			IJ.doCommand("Clear Results");
		else {
			selectAll();
			clearSelection();
		}
	}

	/** Deletes the selected lines. */
	public void clearSelection() {

		if(selStart == -1 || selEnd == -1) {
			if(getLineCount() > 0)
				IJ.error("Text selection required");
			return;
		}
		if(IJ.recording()) {
			if(Recorder.scriptMode())
				Recorder.recordString("IJ.deleteRows(" + selStart + ", " + selEnd + ");\n");
			else {
				if("Results".equals(title))
					Recorder.record("Table.deleteRows", selStart, selEnd);
				else
					Recorder.record("Table.deleteRows", selStart, selEnd, title);
			}
		}
		int first = selStart, last = selEnd, rows = iRowCount;
		if(selStart == 0 && selEnd == (iRowCount - 1)) {
			vData.removeAllElements();
			iRowCount = 0;
			if(rt != null) {
				if(IJ.isResultsWindow() && IJ.getTextPanel() == this) {
					Analyzer.setUnsavedMeasurements(false);
					Analyzer.resetCounter();
				} else
					rt.reset();
			}
		} else {
			int rowCount = iRowCount;
			boolean atEnd = rowCount - selEnd < 8;
			int count = selEnd - selStart + 1;
			for(int i = 0; i < count; i++) {
				vData.removeElementAt(selStart);
				iRowCount--;
			}
			if(rt != null && rowCount == rt.size()) {
				for(int i = 0; i < count; i++)
					rt.deleteRow(selStart);
				rt.show(title);
				if(!atEnd) {
					iY = 0;
					tc.repaint();
				}
			}
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		if(imp != null)
			Overlay.updateTableOverlay(imp, first, last, rows);
		selStart = -1;
		selEnd = -1;
		selOrigin = -1;
		selLine = -1;
		adjustVScroll();
		tc.repaint();
	}

	/** Deletes all the lines. */
	public synchronized void clear() {

		if(vData == null)
			return;
		vData.removeAllElements();
		iRowCount = 0;
		selStart = -1;
		selEnd = -1;
		selOrigin = -1;
		selLine = -1;
		adjustVScroll();
		tc.repaint();
	}

	/** Selects all the lines in this TextPanel. */
	public void selectAll() {

		if(selStart == 0 && selEnd == iRowCount - 1) {
			resetSelection();
			IJ.showStatus("");
			return;
		}
		selStart = 0;
		selEnd = iRowCount - 1;
		selOrigin = 0;
		tc.repaint();
		selLine = -1;
		showLinePos();
	}

	/** Clears the selection, if any. */
	public void resetSelection() {

		selStart = -1;
		selEnd = -1;
		selOrigin = -1;
		selLine = -1;
		if(iRowCount > 0)
			tc.repaint();
	}

	/** Creates a selection and insures it is visible. */
	public void setSelection(int startLine, int endLine) {

		sbVert.setSelection(startLine);
		/* Needed for SWT? */
		/*
		 * if (startLine>endLine) endLine = startLine; if (startLine<0) startLine = 0;
		 * if (endLine<0) endLine = 0; if (startLine>=iRowCount) startLine =
		 * iRowCount-1; if (endLine>=iRowCount) endLine = iRowCount-1; selOrigin =
		 * startLine; selStart = startLine; selEnd = endLine; int vstart = int visible =
		 * sbVert.getVisibleAmount()-1; if (startLine<vstart) {
		 * sbVert.setSelection(startLine); iY=iRowHeight*startLine; } else if
		 * (endLine>=vstart+visible) { vstart = endLine - visible + 1; if (vstart<0)
		 * vstart = 0; sbVert.setSelection(vstart); iY=iRowHeight*vstart; }
		 */
		tc.repaint();
	}

	/** Updates the vertical scroll bar so that the specified row is visible. */
	public void showRow(int rowIndex) {

		showCell(rowIndex, null);
	}

	/** Updates the scroll bars so that the specified cell is visible. */
	public void showCell(int rowIndex, String column) {

		if(rowIndex < 0)
			rowIndex = 0;
		if(rowIndex >= iRowCount)
			rowIndex = iRowCount - 1;
		sbVert.setSelection(rowIndex);
		iY = iRowHeight * sbVert.getSelection();
		/* Changed for SWT! */
		// int hstart = sbHoriz.getSelection();
		// int hVisible = sbHoriz.getVisibleAmount()-1;
		int col = 0;
		if(column != null && sColHead != null && iColWidth != null) {
			for(int i = 0; i < sColHead.length; i++) {
				if(column.equals(sColHead[i])) {
					for(int j = 0; j < i; j++)
						col += iColWidth[j];
					break;
				}
			}
		}
		sbHoriz.setSelection(col);
		iX = col;
		tc.repaint();
	}

	/** Writes all the text in this TextPanel to a file. */
	public void save(PrintWriter pw) {

		resetSelection();
		if(labels != null && !labels.equals("")) {
			String labels2 = labels;
			if(saveAsCSV)
				labels2 = labels2.replaceAll("\t", ",");
			pw.println(labels2);
		}
		for(int i = 0; i < iRowCount; i++) {
			char[] chars = (char[])(vData.elementAt(i));
			String s = new String(chars);
			if(s.endsWith("\t"))
				s = s.substring(0, s.length() - 1);
			if(saveAsCSV)
				s = s.replaceAll("\t", ",");
			pw.println(s);
		}
		unsavedLines = false;
	}

	/**
	 * Saves the text in this TextPanel to a file. Set 'path' to "" to display a
	 * "save as" dialog. Returns 'false' if the user cancels the dialog.
	 */
	public boolean saveAs(String path) {

		boolean isResults = IJ.isResultsWindow() && IJ.getTextPanel() == this;
		boolean summarized = false;
		if(isResults) {
			String lastLine = iRowCount >= 2 ? getLine(iRowCount - 2) : null;
			summarized = lastLine != null && lastLine.startsWith("Max");
		}
		String fileName = null;
		if(rt != null && rt.size() > 0 && !summarized) {
			if(path == null || path.equals("")) {
				IJ.wait(10);
				String name = isResults ? "Results" : title;
				SaveDialog sd = new SaveDialog("Save Table", name, Prefs.defaultResultsExtension());
				fileName = sd.getFileName();
				if(fileName == null)
					return false;
				path = sd.getDirectory() + fileName;
			}
			rt.saveAndRename(path);
			TextWindow tw = getTextWindow();
			String title2 = rt.getTitle();
			if(tw != null && !"Results".equals(title)) {
				tw.getShell().setText(title2);
				Menus.updateWindowMenuItem(title, title2);
				title = title2;
			}
		} else {
			if(path.equals("")) {
				IJ.wait(10);
				boolean hasHeadings = !getColumnHeadings().equals("");
				String ext = isResults || hasHeadings ? Prefs.defaultResultsExtension() : ".txt";
				SaveDialog sd = new SaveDialog("Save as Text", title, ext);
				String file = sd.getFileName();
				if(file == null)
					return false;
				path = sd.getDirectory() + file;
			}
			PrintWriter pw = null;
			try {
				FileOutputStream fos = new FileOutputStream(path);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				pw = new PrintWriter(bos);
			} catch(IOException e) {
				IJ.error("Save As>Text", e.getMessage());
				return true;
			}
			saveAsCSV = path.endsWith(".csv");
			save(pw);
			saveAsCSV = false;
			pw.close();
		}
		if(isResults) {
			Analyzer.setUnsavedMeasurements(false);
			if(IJ.recording() && !IJ.isMacro())
				Recorder.record("saveAs", "Results", path);
		} else if(rt != null) {
			if(IJ.recording() && !IJ.isMacro())
				Recorder.record("saveAs", "Results", path);
		} else {
			if(IJ.recording() && !IJ.isMacro())
				Recorder.record("saveAs", "Text", path);
		}
		IJ.showStatus("");
		return true;
	}

	/** Returns all the text as a string. */
	public synchronized String getText() {

		if(vData == null)
			return "";
		StringBuffer sb = new StringBuffer();
		if(labels != null && !labels.equals("")) {
			sb.append(labels);
			sb.append('\n');
		}
		for(int i = 0; i < iRowCount; i++) {
			if(vData == null)
				break;
			char[] chars = (char[])(vData.elementAt(i));
			sb.append(chars);
			sb.append('\n');
		}
		return new String(sb);
	}

	public void setTitle(String title) {

		this.title = title;
	}

	/** Returns the number of lines of text in this TextPanel. */
	public int getLineCount() {

		return iRowCount;
	}

	/**
	 * Returns the specified line as a string. The argument must be greater than or
	 * equal to zero and less than the value returned by getLineCount().
	 */
	public String getLine(int index) {

		if(index < 0 || index >= iRowCount)
			throw new IllegalArgumentException("index out of range: " + index);
		return new String((char[])(vData.elementAt(index)));
	}

	/**
	 * Replaces the contents of the specified line, where 'index' must be greater
	 * than or equal to zero and less than the value returned by getLineCount().
	 */
	public void setLine(int index, String s) {

		if(index < 0 || index >= iRowCount)
			throw new IllegalArgumentException("index out of range: " + index);
		if(vData != null) {
			vData.setElementAt(s.toCharArray(), index);
			tc.repaint();
		}
	}

	/**
	 * Returns the index of the first selected line, or -1 if there is no slection.
	 */
	public int getSelectionStart() {

		return selStart;
	}

	/**
	 * Returns the index of the last selected line, or -1 if there is no slection.
	 */
	public int getSelectionEnd() {

		return selEnd;
	}

	/** Sets the ResultsTable associated with this TextPanel. */
	public void setResultsTable(ResultsTable rt) {

		if(IJ.debugMode)
			IJ.log("setResultsTable: " + rt);
		this.rt = rt;
		if(!menusExtended)
			extendMenus();
	}

	/** Returns the ResultsTable associated with this TextPanel, or null. */
	public ResultsTable getResultsTable() {

		if(IJ.debugMode)
			IJ.log("getResultsTable: " + rt);
		return rt;
	}

	/**
	 * Returns the ResultsTable associated with this TextPanel, or attempts to
	 * create one and returns the created table.
	 */
	public ResultsTable getOrCreateResultsTable() {

		if((rt == null || rt.size() == 0) && iRowCount > 0 && labels != null && !labels.equals("")) {
			String tmpDir = IJ.getDir("temp");
			if(tmpDir == null) {
				if(IJ.debugMode)
					IJ.log("getOrCreateResultsTable: tmpDir null");
				return null;
			}
			String path = tmpDir + "temp-table.csv";
			saveAs(path);
			try {
				rt = ResultsTable.open(path);
				new File(path).delete();
			} catch(Exception e) {
				rt = null;
				if(IJ.debugMode)
					IJ.log("getOrCreateResultsTable: " + e);
			}
		}
		if(IJ.debugMode)
			IJ.log("getOrCreateResultsTable: " + rt);
		return rt;
	}

	private void extendMenus() {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				new org.eclipse.swt.widgets.MenuItem(pm, SWT.SEPARATOR);
				addPopupItem("Rename...");
				addPopupItem("Duplicate...");
				addPopupItem("Apply Macro...");
				addPopupItem("Sort...");
				addPopupItem("Plot...");
				if(fileMenu != null) {
					// fileMenu.add("Rename...");
					org.eclipse.swt.widgets.MenuItem renameItem = new org.eclipse.swt.widgets.MenuItem(fileMenu, SWT.PUSH);
					renameItem.setText("Rename...");
					// fileMenu.add("Duplicate...");
					org.eclipse.swt.widgets.MenuItem duplicateItem = new org.eclipse.swt.widgets.MenuItem(fileMenu, SWT.PUSH);
					duplicateItem.setText("Duplicate...");
				}
				if(editMenu != null) {
					new org.eclipse.swt.widgets.MenuItem(editMenu, SWT.SEPARATOR);
					// editMenu.addSeparator();
					// editMenu.add("Apply Macro...");
					org.eclipse.swt.widgets.MenuItem applyMacroItem = new org.eclipse.swt.widgets.MenuItem(editMenu, SWT.PUSH);
					applyMacroItem.setText("Apply Macro...");
				}
				menusExtended = true;
			}
		});
	}

	public void scrollToTop() {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				sbVert.setSelection(0);
				iY = 0;
				for(int i = 0; i < iColCount; i++)
					tc.calcAutoWidth(i);
				adjustHScroll();
				tc.repaint();
			}
		});
	}

	void flush() {

		if(vData != null)
			vData.removeAllElements();
		vData = null;
	}

	private void sort() {

		ResultsTable rt2 = getOrCreateResultsTable();
		if(rt2 == null)
			return;
		String[] headers = rt2.getHeadings();
		String[] headers2 = headers;
		if(headers[0].equals("Label")) {
			headers = new String[headers.length - 1];
			for(int i = 0; i < headers.length; i++)
				headers[i] = headers2[i + 1];
		}
		GenericDialog gd = new GenericDialog("Sort Table");
		gd.addChoice("Column: ", headers, headers[0]);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		String column = gd.getNextChoice();
		rt2.sort(column);
		rt2.show(title);
		scrollToTop();
		if(IJ.recording())
			Recorder.record("Table.sort", column);
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEnter(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
}