OSCLearn { // do we need to add avg filter? or time filter

	var parent, bounds;

	var nFocus, check;

	// gui on parent
	var button, resizeValue;

	// local gui
	var win, text, <>order, <learn, <>low, <>high, <>pool;

	// information arrays; allGUI: all possible controls from auto's parent window
	var numGUI, allGUI;

	// local function
	var isControllable, isOpened, isAlt;

	// osc
	var <>oscPath, <>oscTemp, <>oscMapOrder, <>oscMapLowValue, <>oscMapHighValue;

	*new {

		arg parent, bounds;

		^super.newCopyArgs(parent,bounds).initOSCLearn;
	}

	initOSCLearn {

		allGUI = [];

		// define some functions
		isControllable = {|item|
			var result, com;
			com = item.asString;
			result = (com == "a Knob");
			result = result || (com == "a Slider");
			result = result || (com == "a RangeSlider");
			result;
		};

		//gui
		button = Button(parent, bounds)
		.font_(Font(\calibri))
		.states_([
			["OSC Learn", Color.black, Color(0,0,0,0.5)]
		])
		.canFocus_(false)
		.mouseUpAction_({
			arg view, x, y, mod;
			var nLocal; // this ID is local in different window

			if (allGUI.size == 0, { // this is important so that all the things in init below will just be done onece

				// recursively collect all GUIs
				check = { |aParentView|
					aParentView.children.do{ |item|
						if (item.asString == "a CompositeView", {
							check.value(item)
						}, {
							if ( isControllable.value(item) ) {allGUI = allGUI.add(item)}
						});
					};
				};

				check.value(parent.view);

				// init: make copies for each gui
				numGUI = allGUI.size;
				win = {nil}!numGUI;
				text = {nil}!numGUI;
				order = {nil}!numGUI;
				learn = {nil}!numGUI;
				low = {nil}!numGUI;
				high = {nil}!numGUI;
				pool = []!numGUI; // pool is for saving low and high value
				oscPath = {"/"}!numGUI;
				oscTemp = {nil}!numGUI;
				oscMapOrder = 1!numGUI;
				oscMapLowValue = 0.0!numGUI;
				oscMapHighValue = 1.0!numGUI;
			});

			allGUI.do{
				arg item, count;
				if (item.hasFocus) { nFocus = count };
			}; //exam which item is focused

			nLocal = nFocus; // pass focus to local variable

			isOpened = false; // judge isOpened by name

			Window.allWindows.do({
				arg eachWin;
				if(eachWin.name == (parent.name++"-OSCLearn-"++nFocus.asString), {
					isOpened = true})
			});

			if (isOpened.not && (isControllable.value(allGUI[nFocus])), {

				win[nLocal] = Window(
					parent.name++"-OSCLearn-"++nFocus.asString, Rect(
					parent.bounds.left + parent.bounds.width + 20,
					parent.bounds.top + parent.bounds.height - 130,
					295,
					130)
				)
				.front
				.alwaysOnTop_(true)
				.acceptsMouseOver_(true);

				win[nLocal].view.minSize_(Size(295, 130));

				win[nLocal].view.decorator_(FlowLayout(win[nLocal].bounds, 10@10, 10@10));

				// end of decoration

				StaticText(win[nLocal], 35@30)
				.string_("Path:");

				text[nLocal] = TextField(win[nLocal], 230@30)
				.value_(oscPath[nLocal])
				.action_({
					oscPath[nLocal] = text[nLocal].value;
					pool[nLocal] = []; // reset
				});

				StaticText(win[nLocal], 35@30)
				.string_("Item:");

				order[nLocal] = NumberBox(win[nLocal], 20@30)
				.clipLo_(1)
				.value_(oscMapOrder[nLocal])
				.maxDecimals_(1)
				.action_({
					oscMapOrder[nLocal] = order[nLocal].value;
				});

				oscMapOrder[nLocal] = order[nLocal].value;

				StaticText(win[nLocal], 35@30)
				.string_("Low:");

				low[nLocal] = NumberBox(win[nLocal], 50@30)
				.value_(oscMapLowValue[nLocal])
				.minDecimals_(1)
				.maxDecimals_(2)
				.action_({
					oscMapLowValue[nLocal] = low[nLocal].value;

					if (pool[nLocal].size == 0, {
						pool[nLocal] = [low[nLocal].value, high[nLocal].value]
					}, {
						pool[nLocal][0] = low[nLocal].value;
					})

				});

				StaticText(win[nLocal], 35@30)
				.string_("High:");

				high[nLocal] = NumberBox(win[nLocal], 50@30)
				.value_(oscMapHighValue[nLocal])
				.minDecimals_(1)
				.maxDecimals_(2)
				.action_({
					oscMapHighValue[nLocal] = high[nLocal].value;
					if (pool[nLocal].size == 0, {
						pool[nLocal] = [low[nLocal].value, high[nLocal].value]
					}, {
						pool[nLocal][0] = high[nLocal].value;
					})
				});

				learn[nLocal] = Button(win[nLocal], 100@30)
				.states_([
					["Learn", Color.black, Color(0,0,0,0.5)],
					["Learn", Color.black, Color.green]
				])
				.canFocus_(false)
				.action_({

					// in case the user did not press enter key
					oscPath[nLocal] = text[nLocal].value;
					pool[nLocal] = [];

					if (learn[nLocal].value == 1, {

						// learn the high and low value of input osc
						oscTemp[nLocal] = OSCdef(
							(parent.name++"-OSCLearn-"++nFocus).asSymbol,
							{
								arg msg;

								{
									if (pool[nLocal].size < 2, {
										pool[nLocal] = pool[nLocal].add(msg[oscMapOrder[nLocal]])
									}, {

										if (pool[nLocal][0] > msg[oscMapOrder[nLocal]], {
											pool[nLocal][0]=msg[oscMapOrder[nLocal]]
										});

										if (pool[nLocal][1] < msg[oscMapOrder[nLocal]], {
											pool[nLocal][1]=msg[oscMapOrder[nLocal]]
										});

										low[nLocal].value_(pool[nLocal][0]);
										high[nLocal].value_(pool[nLocal][1]);

									});

								}.defer;
							},
							oscPath[nLocal].asSymbol
						);

						parent.view.onClose = {oscTemp[nLocal].free};
					}, {
						// map the learned low and high to the gui

						oscMapLowValue[nLocal] = low[nLocal].value;
						oscMapHighValue[nLocal] = high[nLocal].value;

						OSCdef(
							(parent.name++"-OSCLearn-"++nFocus).asSymbol,
							{
								arg msg;
								// straight mapping
								{
									allGUI[nLocal].valueAction_(
										msg[oscMapOrder[nLocal]].round(0.01)
										.linlin(oscMapLowValue[nLocal],
											oscMapHighValue[nLocal],0,1)
									);
								}.defer;
						}); // why there is no address here?
					});
				});
			}) // end of if isOpened & isControllable;
		});
	} // end of initOSCLearn

	resize_ {
		arg resizeValue;
		button.resize_(resizeValue);
	}
}