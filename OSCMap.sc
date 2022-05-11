OSCMap {

	var parent, bounds;

	var check, isControllable;

	// gui on parent
	var button, resizeValue;

	// local gui
	var win, pop, add;

	// all controllable GUIs in parent window
	var numGUI, allGUI;

	*new {

		arg parent, bounds;

		^super.newCopyArgs(parent,bounds).initOSCMap;
	}

	initOSCMap {

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

		check = { |aParentView|
			aParentView.children.do{ |item|
				if (item.asString == "a CompositeView") { check.value(item) };
				if ( isControllable.value(item) ) {allGUI = allGUI.add(item)};
			};
		};

		//gui
		button = Button(parent, bounds)
		.font_(Font(\calibri))
		.string_("OSC Map")
		.canFocus_(false)
		.action_({
			if (allGUI.size == 0, {

				// recursively collect all GUIs
				check.value(parent.view);

				// init
				numGUI = allGUI.size;
				pop = {nil}!numGUI;

				if (~oscMapOrder.isNil) {~oscMapOrder = {nil}!numGUI};
				if (~oscMapLowValue.isNil) {~oscMapLowValue = {nil}!numGUI};
				if (~oscMapHighValue.isNil) {~oscMapHighValue = {nil}!numGUI};
			});

			Window.allWindows.do({
				arg w;
				if ( w.name == (parent.name++"-OSCMap") ) { "Alread opened.".error };
			});

			win = Window(
				parent.name++"-OSCMap", Rect(
					parent.bounds.left + parent.bounds.width + 20,
					parent.bounds.top + parent.bounds.height - 280,
					400,
					300)
			)
			.alwaysOnTop_(true)
			.acceptsMouseOver_(true)
			.front;

			win.view.minSize_(Size(400, 300));

			win.view.decorator_(FlowLayout(win.bounds, 10@10, 10@10));

			// end of decoration

			/*if (allGUI != 0, {
			pop = PopUpMenu(win, 100@30)
			.items_(allGUI);
			});*/

			pop = PopUpMenu(win, 100@30)
			.items_( allGUI.collect( { |x, i| x.asString.replace("a ", i.asString ++ " - ") } ) )
			.action_({
				arg view;
				var move;

				move = {
					arg totalDistance;
					var t = allGUI[view.value].value;
					Routine {
						100.do{
							allGUI[view.value].value_(t);
							t = t + (totalDistance / 100);
							0.00001.wait;
						};
						0.00001.wait;
						100.do{
							allGUI[view.value].value_(t);
							t = t - (totalDistance / 100);
							0.00001.wait;
						}
					}.play(AppClock)
				};

				if (allGUI[view.value].value > 0.5,{
					move.(-0.4);
				},{
					move.(0.4);
				});
			});	// end of pop action

			add = Button(win, 100@30)
			.action_({
				var c;
				c = CompositeView(win, 300@30)
				.backColor_(Color(0,0,0,0.3));
				c.decorator_(FlowLayout(c.bounds, 10@10, 10@10));
				StaticText(c, 100@10)
				.string_( allGUI[pop.value].asString.replace("a ", pop.value.asString ++ " - ") );

			});
		}); //end of button action
	} // end of init

	resize_ {
		arg resizeValue;
		button.resize_(resizeValue);
	}
}