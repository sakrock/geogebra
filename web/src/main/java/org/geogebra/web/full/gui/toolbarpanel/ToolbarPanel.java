package org.geogebra.web.full.gui.toolbarpanel;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.euclidian.MyModeChangedListener;
import org.geogebra.common.euclidian.event.PointerEventType;
import org.geogebra.common.io.layout.PerspectiveDecoder;
import org.geogebra.common.javax.swing.SwingConstants;
import org.geogebra.common.main.App;
import org.geogebra.common.main.App.InputPosition;
import org.geogebra.web.full.css.MaterialDesignResources;
import org.geogebra.web.full.gui.applet.GeoGebraFrameBoth;
import org.geogebra.web.full.gui.exam.ExamUtil;
import org.geogebra.web.full.gui.layout.DockManagerW;
import org.geogebra.web.full.gui.layout.DockSplitPaneW;
import org.geogebra.web.full.gui.layout.GUITabs;
import org.geogebra.web.full.gui.layout.panels.AlgebraDockPanelW;
import org.geogebra.web.full.gui.layout.panels.ToolbarDockPanelW;
import org.geogebra.web.full.gui.view.algebra.AlgebraViewW;
import org.geogebra.web.full.main.AppWFull;
import org.geogebra.web.html5.gui.FastClickHandler;
import org.geogebra.web.html5.gui.tooltip.ToolTipManagerW;
import org.geogebra.web.html5.gui.util.AriaHelper;
import org.geogebra.web.html5.gui.util.ClickStartHandler;
import org.geogebra.web.html5.gui.util.MathKeyboardListener;
import org.geogebra.web.html5.gui.util.StandardButton;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.Dom;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Laszlo Gal
 *
 */
public class ToolbarPanel extends FlowPanel implements MyModeChangedListener {
	/** vertical offset for shadow */
	public static final int VSHADOW_OFFSET = 4;

	private static final int MIN_ROWS_WITHOUT_KEYBOARD = 5;

	private static final int MIN_ROWS_WITH_KEYBOARD = 3;

	private static final int HDRAGGER_WIDTH = 8;

	private static final int OPEN_ANIM_TIME = 200;

	/** Closed width of header in landscape mode */
	public static final int CLOSED_WIDTH_LANDSCAPE = 56;

	/** Min width of open header in landscape mode */
	public static final int OPEN_MIN_WIDTH_LANDSCAPE = 160;

	/** Closed height of header in portrait mode */
	public static final int CLOSED_HEIGHT_PORTRAIT = 56;

	/** Application */
	AppW app;

	/**
	 * Tab ids.
	 */
	enum TabIds {
		/** tab one */
		ALGEBRA,

		/** tab two */
		TOOLS,

		/** tab three */
		TABLE
	}

	/** Header of the panel with buttons and tabs */
	Header header;

	private FlowPanel main;
	private int tabCount = 0;
	private StandardButton moveBtn;
	private Integer lastOpenWidth = null;
	private AlgebraTab tabAlgebra = null;
	private ToolsTab tabTools = null;
	private TabIds selectedTabId;
	private boolean closedByUser = false;
	private ScheduledCommand deferredOnRes = new ScheduledCommand() {
		@Override
		public void execute() {
			resize();
		}
	};

	/**
	 * Selects MODE_MOVE as mode and changes visual settings accordingly of
	 * this.
	 */
	public void setMoveMode() {
		tabTools.setMoveMode();
	}

	/**
	 * Changes visual settings of selected mode.
	 * 
	 * @param mode
	 *            the mode will be selected
	 */
	public void setMode(int mode) {
		tabTools.setMode(mode);
	}

	/**
	 * Updates the style of undo and redo buttons accordingly of they are active
	 * or inactive
	 */
	public void updateUndoRedoActions() {
		header.updateUndoRedoActions();
	}

	/**
	 * Updates the position of undo and redo buttons
	 */
	public void updateUndoRedoPosition() {
		header.updateUndoRedoPosition();
	}

	/**
	 * Base class for Toolbar Tabs-
	 * 
	 * @author Laszlo
	 *
	 */
	abstract static class ToolbarTab extends ScrollPanel {
		/** Constructor */
		public ToolbarTab() {
			setSize("100%", "100%");
			setAlwaysShowScrollBars(false);
		}

		@Override
		public void onResize() {
			setHeight("100%");
		}

		/**
		 * Set tab the active one.
		 * 
		 * @param active
		 *            to set.
		 */
		public void setActive(boolean active) {
			if (active) {
				removeStyleName("tab-hidden");
				addStyleName("tab");
			} else {
				removeStyleName("tab");
				addStyleName("tab-hidden");
			}
		}

		/**
		 * Sets if tab should fade during animation or not.
		 * 
		 * @param fade
		 *            to set.
		 */
		public void setFade(boolean fade) {
			setStyleName("tabFade", fade);
		}
	}

	/**
	 * 
	 * @param app
	 *            .
	 */
	public ToolbarPanel(AppW app) {
		this.app = app;
		app.getActiveEuclidianView().getEuclidianController()
				.setModeChangeListener(this);
		initGUI();
		initClickStartHandler();
	}

	private void add(ToolbarTab tab) {
		tab.addStyleName("tab");
		main.add(tab);
		tabCount++;
	}

	/**
	 * 
	 * @return width of one tab.
	 */
	public int getTabWidth() {
		int w = header.getOffsetWidth();
		if (isAnimating() && !app.isPortrait()) {
			w -= HDRAGGER_WIDTH;
		}
		return w > 0 ? w : 0;
	}

	private void initClickStartHandler() {
		ClickStartHandler.init(this, new ClickStartHandler() {
			@Override
			public void onClickStart(final int x, final int y,
					PointerEventType type) {
				app.getActiveEuclidianView().getEuclidianController()
						.closePopups(x, y, type);
			}
		});
	}

	/**
	 * Init gui, don't open any panels
	 */
	private void initGUI() {
		clear();
		addStyleName("toolbar");
		header = new Header(this, app);
		add(header);
		main = new FlowPanel();
		sinkEvents(Event.ONCLICK);
		main.addStyleName("main");
		tabAlgebra = new AlgebraTab(this);
		tabTools = new ToolsTab(this);
		add(tabAlgebra);
		add(tabTools);
		addMoveBtn();
		add(main);
		ClickStartHandler.initDefaults(main, false, true);
		hideDragger();
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONCLICK
				&& app.isMenuShowing()) {
			toggleMenu();
		}
		super.onBrowserEvent(event);
	}

	/**
	 * 
	 * @return the height of open toolbar in portrait mode.
	 */
	int getOpenHeightInPortrait() {
		double h = app.getHeight();
		int kh = 0;
		if (app.isUnbundledGraphing() || app.isUnbundled3D()) {
			return (int) (Math
					.round(h * PerspectiveDecoder.portraitRatio(h, true))) + kh;
		}
		return (int) (Math
				.round(h * PerspectiveDecoder.portraitRatio(h, false)));
	}

	/**
	 * resets toolbar
	 */
	public void reset() {
		lastOpenWidth = null;
		hideDragger();
		header.reset();
		resizeTabs();
	}

	private void addMoveBtn() {
		moveBtn = new StandardButton(
				MaterialDesignResources.INSTANCE.mode_move(), null, 24,
				app);
		AriaHelper.hide(moveBtn);
		String altText = app.getLocalization().getMenu(
				EuclidianConstants.getModeText(EuclidianConstants.MODE_MOVE))
				+ ". " + app.getToolHelp(EuclidianConstants.MODE_MOVE);
		moveBtn.setTitle(altText);
		moveBtn.setAltText(altText);
		moveBtn.setStyleName("moveFloatingBtn");
		// moveMoveBtnDown style added for moveBtn to fix the position on tablet
		// too
		moveBtn.setIgnoreTab();
		moveBtn.addStyleName("moveMoveBtnDown");
		main.add(moveBtn);
		hideMoveFloatingButton();
		FastClickHandler moveBtnHandler = new FastClickHandler() {
			
			@Override
			public void onClick(Widget source) {
				moveBtnClicked();
			}
		};
		moveBtn.addFastClickHandler(moveBtnHandler);
	}

	/**
	 * Handler for move floating button
	 */
	protected void moveBtnClicked() {
		setMoveMode();
		tabTools.showTooltip(EuclidianConstants.MODE_MOVE);
	}

	private void hideDragger() {
		ToolbarDockPanelW dockPanel = getToolbarDockPanel();
		final DockSplitPaneW dockParent = dockPanel != null
				? dockPanel.getParentSplitPane() : null;
		if (dockParent != null) {
			final Widget opposite = dockParent.getOpposite(dockPanel);
			dockParent.addStyleName("hide-Dragger");
			if (opposite != null) {
				Dom.toggleClass(opposite, "hiddenHDraggerRightPanel", dockParent
						.getOrientation() == SwingConstants.HORIZONTAL_SPLIT);
			}
		}
	}

	/**
	 * Opens the toolbar.
	 */
	public void doOpen() {
		if (header.isOpen()) {
			return;
		}
		setClosedByUser(false);
		header.setOpen(true);
	}

	/**
	 * Closes the toolbar.
	 */
	public void close() {
		if (!header.isOpen()) {
			return;
		}
		header.setOpen(false);
	}

	/**
	 * updates panel width according to its state in landscape mode.
	 */
	public void updateWidth() {
		if (app.isPortrait()) {
			return;
		}
		final ToolbarDockPanelW dockPanel = getToolbarDockPanel();
		final DockSplitPaneW dockParent = dockPanel != null
				? dockPanel.getParentSplitPane() : null;
		if (dockPanel != null && getLastOpenWidth() != null) {
			final Widget opposite = dockParent.getOpposite(dockPanel);
			AnimationCallback animCallback = null;
			dockParent.addStyleName("hide-Dragger");
			opposite.addStyleName("hiddenHDraggerRightPanel");
			if (header.isOpen()) {
				dockParent.setWidgetSize(dockPanel,
						getLastOpenWidth().intValue());
				animCallback = new LandscapeAnimationCallback(header,
						CLOSED_WIDTH_LANDSCAPE, getLastOpenWidth());
			} else {
				lastOpenWidth = getOffsetWidth();
				dockParent.setWidgetMinSize(dockPanel, CLOSED_WIDTH_LANDSCAPE);
				dockParent.setWidgetSize(dockPanel, CLOSED_WIDTH_LANDSCAPE);
				animCallback = new LandscapeAnimationCallback(header,
						getLastOpenWidth(),
						CLOSED_WIDTH_LANDSCAPE) {

					@Override
					public void onEnd() {
						super.onEnd();
						dockParent.addStyleName("hide-HDragger");
						opposite.addStyleName("hiddenHDraggerRightPanel");
					}
				};
			}
			dockParent.animate(OPEN_ANIM_TIME, animCallback);
		}
	}

	private void setMinimumSize() {
		ToolbarDockPanelW dockPanel = getToolbarDockPanel();
		DockSplitPaneW dockParent = dockPanel != null
				? dockPanel.getParentSplitPane() : null;
		if (dockParent != null) {
			dockParent.setWidgetMinSize(dockPanel,
					header.isOpen() ? OPEN_MIN_WIDTH_LANDSCAPE
							: CLOSED_WIDTH_LANDSCAPE);
		}
	}

	/**
	 * updates panel height according to its state in portrait mode.
	 */
	public void updateHeight() {
		if (!app.isPortrait()) {
			return;
		}
		ToolbarDockPanelW dockPanel = getToolbarDockPanel();
		final DockSplitPaneW dockParent = dockPanel != null
				? dockPanel.getParentSplitPane() : null;
		if (dockPanel != null) {
			Widget evPanel = dockParent.getOpposite(dockPanel);
			if (header.isOpen()) {
				dockParent.setWidgetSize(evPanel, getOpenHeightInPortrait());
				dockParent.removeStyleName("hide-VDragger");
			} else {
				dockParent.setWidgetSize(evPanel,
						app.getHeight() - header.getOffsetHeight()
								- app.getArticleElement().getBorderThickness()
								- VSHADOW_OFFSET);
				dockParent.addStyleName("hide-VDragger");
			}

			dockParent.animate(OPEN_ANIM_TIME,
					new PortraitAnimationCallback(header, app) {
						@Override
						protected void onEnd() {
							super.onEnd();
							dockParent.forceLayout();
						}
					});
		}
	}

	/**
	 * @return algebra dock panel
	 */
	ToolbarDockPanelW getToolbarDockPanel() {
		return (ToolbarDockPanelW) app.getGuiManager().getLayout()
				.getDockManager().getPanel(App.VIEW_ALGEBRA);
	}

	/**
	 * @return mode floating action button
	 */
	public StandardButton getMoveBtn() {
		return moveBtn;
	}

	@Override
	public void onModeChange(int mode) {
		updateMoveButton(mode);
	}

	/**
	 * show or hide move btn according to selected tool
	 */
	public void updateMoveButton() {
		updateMoveButton(app.getMode());
	}

	private void updateMoveButton(int mode) {
		if (mode == EuclidianConstants.MODE_MOVE) {
			hideMoveFloatingButton();
		} else {
			showMoveFloatingButton();
		}
	}

	/**
	 * Show move floating action button
	 */
	void showMoveFloatingButton() {
		if (moveBtn == null) {
			return;
		}
		moveBtn.addStyleName("showMoveBtn");
		moveBtn.removeStyleName("hideMoveBtn");
		moveBtn.setTabIndex(GUITabs.NO_TAB);
	}

	/**
	 * Hide move floating action button
	 */
	public void hideMoveFloatingButton() {
		if (moveBtn == null) {
			return;
		}
		moveBtn.addStyleName("hideMoveBtn");
		moveBtn.removeStyleName("showMoveBtn");
		moveBtn.setTabIndex(GUITabs.NO_TAB);
	}

	/**
	 * @param ttLeft
	 *            - tooltip left
	 * @param width
	 *            - width
	 * @param isSmall
	 *            - is small tooltip
	 * @return true if was moved
	 */
	public boolean moveMoveFloatingButtonUpWithTooltip(int ttLeft, int width,
			boolean isSmall) {
		if (moveBtn != null) {
			int mLeft = moveBtn.getAbsoluteLeft()
					- (int) app.getAbsLeft();
			int mRight = mLeft + 48;
			int ttRight = ttLeft + width;
			if ((ttLeft < mRight && ttRight > mRight)
					|| (ttRight > mLeft && ttLeft < mLeft)) {
				if (isSmall) {
					moveBtn.removeStyleName("moveMoveBtnDownSmall");
					moveBtn.addStyleName("moveMoveBtnUpSmall");
				} else {
					moveBtn.removeStyleName("moveMoveBtnDown");
					moveBtn.addStyleName("moveMoveBtnUp");
				}
				return true; // button was moved
			}
		}
		return false; // button was not moved
	}

	/**
	 * @param isSmall
	 *            - is small tooltip
	 * @param wasMoved
	 *            - true if was moved
	 */
	public void moveMoveFloatingButtonDownWithTooltip(boolean isSmall,
			boolean wasMoved) {
		if (moveBtn != null && wasMoved) {

			if (isSmall) {
				moveBtn.addStyleName("moveMoveBtnDownSmall");
				moveBtn.removeStyleName("moveMoveBtnUpSmall");
			} else {
				moveBtn.addStyleName("moveMoveBtnDown");
				moveBtn.removeStyleName("moveMoveBtnUp");
			}
		}
	}

	/**
	 * @return if toolbar is open or not.
	 */
	public boolean isOpen() {
		return header.isOpen();
	}

	/**
	 * Just for convince.
	 * 
	 * @return if toolbar is closed or not.
	 */
	public boolean isClosed() {
		return !isOpen();
	}

	/**
	 * @return the frame with casting.
	 */
	GeoGebraFrameBoth getFrame() {
		return (((AppWFull) app).getAppletFrame());
	}

	/**
	 * @param expanded
	 *            whether menu is open
	 */
	public void markMenuAsExpanded(boolean expanded) {
		header.markMenuAsExpanded(expanded);
	}

	/**
	 * @param b
	 *            To show or hide keyboard button.
	 */
	void showKeyboardButtonDeferred(final boolean b) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				getFrame().showKeyboardButton(b);

			}
		});
	}

	/**
	 * 
	 * @return the last width when toolbar was open.
	 */
	Integer getLastOpenWidth() {
		return lastOpenWidth;
	}

	/**
	 * 
	 * @param value
	 *            to set.
	 */
	void setLastOpenWidth(Integer value) {
		this.lastOpenWidth = value;
	}

	/**
	 * Opens and closes Burger Menu
	 */
	void toggleMenu() {
		app.toggleMenu();
	}

	/**
	 * Opens algebra tab.
	 * 
	 * @param fade
	 *            decides if tab should fade during animation.
	 */
	public void openAlgebra(boolean fade) {
		header.selectTab(TabIds.ALGEBRA);
		open();
		main.addStyleName("algebra");
		main.removeStyleName("tools");

		tabAlgebra.setActive(true);
		tabTools.setActive(false);
		setFadeTabs(fade);
		hideMoveFloatingButton();
		setMoveMode();
	}

	/**
	 * Opens tools tab.
	 * 
	 * @param fade
	 *            decides if tab should fade during animation.
	 */
	public void openTools(boolean fade) {
		if (!app.showToolBar()) {
			openAlgebra(fade);
			return;
		}
		ToolTipManagerW.hideAllToolTips();
		header.selectTab(TabIds.TOOLS);
		open();
		main.removeStyleName("algebra");
		main.addStyleName("tools");
		tabAlgebra.setActive(false);
		tabTools.setActive(true);
		setFadeTabs(fade);
		updateMoveButton();
	}

	/**
	 * Opens tools tab.
	 * 
	 * @param fade
	 *            decides if tab should fade during animation.
	 */
	public void openTableView(boolean fade) {
		if (!app.showToolBar()) {
			openAlgebra(fade);
			return;
		}
		ToolTipManagerW.hideAllToolTips();
		header.selectTab(TabIds.TABLE);
		open();
		/*
		 * main.removeStyleName("algebra"); main.addStyleName("tools");
		 * tabAlgebra.setActive(false); tabTools.setActive(true);
		 * setFadeTabs(fade); updateMoveButton();
		 */
	}

	/**
	 * @return tool tab
	 */
	public ToolsTab getTabTools() {
		return tabTools;
	}

	private void open() {
		if (!isOpen()) {
			doOpen();
		}
		onOpen();
	}

	/**
	 * Called after open.
	 */
	protected void onOpen() {
		resizeTabs();
		main.getElement().getStyle().setProperty("height", "calc(100% - 56px)");
		main.getElement().getStyle().setProperty("width",
				(tabCount * 100) + "%");
	}

	/**
	 * Resize tabs.
	 */
	public void resize() {
		if (getOffsetWidth() == 0) {
			return;
		}

		header.resize();
		resizeTabs();
	}

	private void resizeTabs() {
		if (tabAlgebra != null) {
			tabAlgebra.onResize();
		}

		if (tabTools != null) {
			tabTools.onResize();
		}
	}

	/**
	 * Shows/hides full toolbar.
	 */
	void updateStyle() {
		setMinimumSize();
		if (header.isOpen()) {
			main.removeStyleName("hidden");
		} else {
			main.addStyleName("hidden");
		}
	}

	/**
	 * 
	 * @return true if AV is selected and ready to use.
	 */
	public boolean isAlgebraViewActive() {
		return tabAlgebra != null && selectedTabId == TabIds.ALGEBRA;
	}

	/**
	 * Scrolls to currently edited item, if AV is active.
	 */
	public void scrollToActiveItem() {
		if (isAlgebraViewActive()) {
			tabAlgebra.scrollToActiveItem();
		}
	}

	/**
	 * 
	 * @return the selected tab id.
	 */
	public TabIds getSelectedTabId() {
		return selectedTabId;
	}

	/**
	 * 
	 * @param tabId
	 *            to set.
	 */
	public void setSelectedTabId(TabIds tabId) {
		this.selectedTabId = tabId;
	}

	/**
	 * 
	 * @return The height that AV should have minimally in portrait mode.
	 */
	public double getMinVHeight() {
		int rows = getFrame().isKeyboardShowing() ? MIN_ROWS_WITH_KEYBOARD
				: MIN_ROWS_WITHOUT_KEYBOARD;
		return rows * header.getOffsetHeight();
	}

	/**
	 * Saves the scroll position of algebra view
	 */
	public void saveAVScrollPosition() {
		tabAlgebra.saveScrollPosition();
	}

	/**
	 * Scrolls to the bottom of AV.
	 */
	public void scrollAVToBottom() {
		if (tabAlgebra != null) {
			tabAlgebra.scrollToBottom();
		}
	}

	/**
	 * @return keyboard listener of AV.
	 * 
	 */
	public MathKeyboardListener getKeyboardListener() {
		if (tabAlgebra == null
				|| app.getInputPosition() != InputPosition.algebraView) {
			return null;
		}
		return ((AlgebraViewW) app.getAlgebraView()).getActiveTreeItem();
	}

	/**
	 * @param ml
	 *            to update.
	 * @return the updated listener.
	 */
	public MathKeyboardListener updateKeyboardListener(
			MathKeyboardListener ml) {
		return AlgebraDockPanelW
				.updateKeyboardListenerForView(this.tabAlgebra.aview, ml);
	}

	/**
	 * 
	 * @return true if toolbar is closed by user with close button, and not by
	 *         code.
	 */
	boolean isClosedByUser() {
		return closedByUser;
	}

	/**
	 * Sets if user closed the toolbar.
	 * 
	 * @param value
	 *            to set
	 */
	void setClosedByUser(boolean value) {
		this.closedByUser = value;
	}

	/**
	 * 
	 * @return if toolbar is animating or not.
	 */
	public boolean isAnimating() {
		return header.isAnimating();
	}

	/**
	 * Resize in a deferred way.
	 */
	public void deferredOnResize() {
		Scheduler.get().scheduleDeferred(deferredOnRes);
	}

	/**
	 * update header style
	 */
	public void updateHeader() {
		header.updateStyle();
	}

	/**
	 * @param style
	 *            style to change color of header (teal -> ok, red -> cheating)
	 */
	public void setHeaderStyle(String style) {
		resetHeaderStyle();
		header.addStyleName(style);
		ExamUtil.makeRed(header.getElement(), "examCheat".equals(style));
	}

	/**
	 * 
	 */
	public void initInfoBtnAction() {
		header.initInfoBtnAction();
	}

	/**
	 * remove exam style
	 */
	public void resetHeaderStyle() {
		ExamUtil.makeRed(header.getElement(), false);
		header.removeStyleName("examOk");
		header.removeStyleName("examCheat");
	}

	/**
	 * Called when app changes orientation.
	 */
	public void onOrientationChange() {
		header.onOrientationChange();
		hideDragger();
	}

	/**
	 * set labels of gui elements
	 */
	public void setLabels() {
		header.setLabels();
		if (!getTabTools().isCustomToolbar) {
			tabTools.toolsPanel.setLabels();
			tabTools.moreBtn
					.setText(app.getLocalization().getMenu("Tools.More"));
			tabTools.lessBtn
					.setText(app.getLocalization().getMenu("Tools.Less"));
		}
		if (moveBtn != null) {
			String altText = app.getLocalization()
					.getMenu(EuclidianConstants
							.getModeText(EuclidianConstants.MODE_MOVE))
					+ ". " + app.getToolHelp(EuclidianConstants.MODE_MOVE);
			moveBtn.setTitle(altText);
			moveBtn.setAltText(altText);
		}
	}

	/**
	 * close portrait
	 */
	public void doCloseInPortrait() {
		DockManagerW dm = (DockManagerW) app.getGuiManager().getLayout()
				.getDockManager();
		dm.closePortrait();
	}

	/**
	 * sets icons tab-able.
	 */
	public void setTabIndexes() {
		header.setTabIndexes();
	}

	/**
	 * Sets if current tab should animate or not.
	 * 
	 * @param fade
	 *            to set.
	 */
	public void setFadeTabs(boolean fade) {
		tabAlgebra.setFade(fade);
		tabTools.setFade(fade);
	}

	/** Sets focus to Burger menu */
	public void focusMenu() {
		header.focusMenu();
	}

	/**
	 * Sets focus to AV Input
	 * 
	 * @param force
	 *            force to open AV tab if not active
	 * 
	 * @return if input can be focused.
	 */
	public boolean focusInput(boolean force) {
		if (force) {
			openAlgebra(true);
		}
		return isAlgebraViewActive() && tabAlgebra.focusInput();
	}
}
