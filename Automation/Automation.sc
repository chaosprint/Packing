Automation {

	var parent, bounds;

	var nFocus;

	// gui on parent
	var button, resizeValue;

	// local gui
	var win, scaleBox, lastPoint, xTxt, yTxt, pop, switchPlay, list, listCV, allCV, textField, yLo, yHi;

	// information arrays; allGUI: all possible controls from auto's parent window
	var numGUI, allGUI, allEnvWin, allEnv, allCurve, allScale, allSwitchPlay, <>yLoVal, <>yHiVal;

	// save and load array

	var listContent, listItems, listValues, scaleT, envLevels, envTimes;

	// automation play and rec
	var playList, recList, recValue, recTime, recGap, playDone;

	// local function
	var isKnobOrSlider, isOpened;

	var <newEnv;

	*new {

		arg parent, bounds;

		^super.newCopyArgs(parent,bounds).init;
	}

	init {

		allGUI = [];

		// define some functions
		isKnobOrSlider = {|item|
			(item.asString == "a Slider") || (item.asString == "a Knob");
		};

		//gui
		button = Button(parent, bounds)
		.font_(Font(\calibri))
		.states_([
			["Automation", Color.black, Color(0,0,0,0.5)]
		])
		.canFocus_(false)
		.action_({

			var nLocal, check; // this ID is local in different window

			if (allGUI.size == 0, {

				// use recursive to collect all GUIs
				check = { |aParentView|
					aParentView.children.do{ |item|
						if (item.asString == "a CompositeView", {
							check.value(item)
						}, {
							if (isKnobOrSlider.value(item), {
								allGUI = allGUI.add(item)
							});
						});
					}
				};

				check.value(parent.view);

				// init
				numGUI = allGUI.size;

				allEnvWin = {nil}!numGUI;
				win = {nil}!numGUI;
				xTxt = {nil}!numGUI;
				yTxt = {nil}!numGUI;
				scaleBox = {nil}!numGUI;
				lastPoint = {nil}!numGUI;
				pop = {nil}!numGUI;
				list = {nil}!numGUI;
				textField = {nil}!numGUI;
				yLo = {nil}!numGUI;
				yHi = {nil}!numGUI;

				yLoVal = {0}!numGUI;
				yHiVal = {1}!numGUI;

				listCV = {nil}!numGUI;
				allCV = {nil}!numGUI;

				listContent = {[]}!numGUI;
				listItems = {[]}!numGUI;
				listValues = {nil}!numGUI;

				allEnv = {Env([0.5, 0.5],[1],\lin)}!numGUI;
				allCurve = {\lin}!numGUI;
				allScale = {1}!numGUI;

				allSwitchPlay = {0}!numGUI;

				recValue = {[]}!numGUI;
				recTime= {[]}!numGUI;
				recGap = {[]}!numGUI;


				playList = {[]}!numGUI;
				recList = {nil}!numGUI;
				playDone = {nil};
			});

			allGUI.do{
				arg item, count;
				if (item.hasFocus, {
					nFocus = count;
				})
			}; //exam which item is focused

			nLocal = nFocus; // pass focus to local variable

			isOpened = false; // judge isOpened by name

			Window.allWindows.do({
				arg eachWin;
				if(eachWin.name == (parent.name++"-automation-"++nFocus.asString), {
					isOpened = true})
			});

			if (isOpened.not && (isKnobOrSlider.value(allGUI[nFocus])), {

				win[nLocal] = Window(parent.name++"-automation-"++nFocus.asString, Rect(
					parent.bounds.left + parent.bounds.width + 20,
					parent.bounds.top + parent.bounds.height - 430,
					490,
					430)
				)
				.front
				.alwaysOnTop_(true)
				.acceptsMouseOver_(true);

				win[nLocal].view.minSize_(Size(490, 430));

				win[nLocal].view.decorator_(FlowLayout(win[nLocal].bounds, 10@10, 10@10));

				// end of decoration

				allEnvWin[nLocal] = EnvDraw(win[nLocal], 470@210)
				.resize_(5).yLo_(yLoVal[nLocal]).yHi_(yHiVal[nLocal])
				.env_(allEnv[nLocal], nLocal)
				.scale_(allScale[nLocal])
				.action_({

					allEnv[nLocal] = allEnvWin[nLocal].env;

					// set the GUI value to be first point
					allGUI[nLocal].valueAction_(allEnvWin[nLocal].env.levels[0]);
					allCurve[nLocal] = allEnvWin[nLocal].curve;

					scaleBox[nLocal].valueAction_(allEnvWin[nLocal].scale);


					if (allCurve[nLocal].isArray, {
						pop[nLocal].value_(2);
					}, {
						if (allCurve[nLocal] == \lin) {pop[nLocal].value_(0)} {pop[nLocal].value_(1)};
					});

				})
				.mouseOverAction_({
					xTxt[nLocal].string_("time:  "++allEnvWin[nLocal].mousePos[0].asString);
					yTxt[nLocal].string_("value: "++allEnvWin[nLocal].mousePos[1].asString);
				});

				// when opened init the GUI value to be first point
				allGUI[nLocal].valueAction_(allEnvWin[nLocal].env.levels[0]);

				listCV[nLocal] = CompositeView(win[nLocal], 230@150).resize_(8);
				listCV[nLocal].decorator_(FlowLayout(listCV[nLocal].bounds, 0@0, 10@10));

				StaticText(listCV[nLocal], 40@30)
				.font_(Font(\calibri))
				.string_("Add As:");

				textField[nLocal] = TextField(listCV[nLocal], 180@30)
				.resize_(8)
				.background_(Color(0,0,0,0))
				.font_(Font(\calibri))
				.action_({
					arg item;

					listItems[nLocal] = listItems[nLocal].add(item.string);
					list[nLocal].items_(listItems[nLocal]);
					list[nLocal].value_(list[nLocal].items.size-1);
					listValues[nLocal] = list[nLocal].value;

					// add an Array in the content array to store info
					listContent[nLocal]  = listContent[nLocal].add([
						allEnvWin[nLocal].env, // 0: env
						allEnvWin[nLocal].scale, // 1: scale
						allEnvWin[nLocal].curve // 2: curves
					]);

					item.string_("");
				});

				list[nLocal] = ListView(listCV[nLocal], 230@110).resize_(8)
				.items_(listItems[nLocal])
				.action_({
					arg theList;
					var env, scale, curve;
					env = listContent[nLocal][theList.value][0];
					scale = listContent[nLocal][theList.value][1];
					curve = listContent[nLocal][theList.value][2];

					// set the GUI and local information;
					this.env_(env, nLocal);

					allGUI[nLocal].valueAction_(allEnvWin[nLocal].env.levels[0]);
				});

				list[nLocal].value_( if (listValues[nLocal] != nil) {listValues[nLocal]} {nil});

				allCV[nLocal] = CompositeView(win[nLocal], 230@150).resize_(9);
				allCV[nLocal].decorator_(FlowLayout(allCV[nLocal].bounds, 0@0, 10@10));

				Button(allCV[nLocal], 50@30).resize_(7).font_(Font(\calibri)).string_("Add")
				.action_({

					// communication with list
					listItems[nLocal] = listItems[nLocal].add(Date.getDate.asString);
					list[nLocal].items_(listItems[nLocal]);
					list[nLocal].value_(list[nLocal].items.size-1);
					listValues[nLocal] = list[nLocal].value;

					// add an Array in the content array to store info
					listContent[nLocal]  = listContent[nLocal].add([
						allEnvWin[nLocal].env, // 0: env
						allEnvWin[nLocal].scale, // 1: scale
						allEnvWin[nLocal].curve // 2: curves
					]);
				});

				Button(allCV[nLocal], 50@30).resize_(7).font_(Font(\calibri)).string_("Del")
				.action_({

					var valueT;

					valueT = list[nLocal].value;

					listItems[nLocal].removeAt(valueT);
					list[nLocal].items_(listItems[nLocal]);
					listContent[nLocal].removeAt(valueT);

					if (valueT != 0) {list[nLocal].value_(valueT - 1)}; // set List highlight position
					if (valueT != 0) {list[nLocal].valueAction_(list[nLocal].value)};
				});

				Button(allCV[nLocal], 110@30).resize_(7).font_(Font(\calibri)).string_("Clean Env")
				.action_({
					this.env_(Env([0.5,0.5],[1]), nLocal);
					pop[nLocal].valueAction_(0);
					recValue = {[]}!numGUI;
					recTime= {[]}!numGUI;
					recGap = {[]}!numGUI;
				});

				Button(allCV[nLocal], 110@30).resize_(7).font_(Font(\calibri)).string_("Save As")
				.action_({
					var file;
					Dialog.savePanel({
						arg filePath;

						file = File(filePath, "w");

						list[nLocal].items.do{
							arg itemName, count;
							file.write(itemName++"\n");

							listContent[nLocal][count][0].levels.do{|i, j|
								file.write(
									if (j != (listContent[nLocal][count][0].levels.size - 1)) {i.asString++" "} {i.asString++"\n"};
								);
							}; // end of writing Env levels

							listContent[nLocal][count][0].times.do{|i, j|
								file.write(
									if (j != (listContent[nLocal][count][0].times.size - 1)) {i.asString++" "} {i.asString++"\n"};
								);
							}; // Env times

							file.write(listContent[nLocal][count][1].asString++"\n"); // scale

							if (listContent[nLocal][count][2].isArray, {

								listContent[nLocal][count][2].do{|i, j|
									file.write(
										if (j != (listContent[nLocal][count][0].times.size - 1)) {i.asString++" "} {i.asString++"\n"};
									);
								}; // write Array curve as Separate Numbers

							},{
								file.write(listContent[nLocal][count][2].asString++"\n"); // curve as String
							});
						};
						file.close; // always remenber to close it
					}, {});
				});
				Button(allCV[nLocal], 110@30).resize_(7).font_(Font(\calibri)).string_("Load Snapshot")
				.action_({
					var file;
					Dialog.openPanel({
						arg filePath;
						file = FileReader.read(filePath);

						listItems[nLocal] = []; // may cause you lose current unsaved list

						listContent[nLocal] = [];

						file.do{
							arg item, count;
							switch (count % 5,
								0, {
									// list item strings
									var itemName;

									item.do{
										arg s, i;
										if ( (i != (item.size - 1)) ,{
											itemName = itemName ++ s ++ " ";
										},{
											itemName = itemName ++ s;
										});
									};

									listItems[nLocal] = listItems[nLocal].add(itemName);
									list[nLocal].items_(listItems[nLocal]);
								},

								1, {
									envLevels = [];
									item.do{
										arg i;
										envLevels = envLevels.add(i.asFloat);
									};
								},

								2, {
									envTimes = [];
									item.do{
										arg i;
										envTimes = envTimes.add(i.asFloat);
									};
								},

								3, {
									scaleT = item[0].asFloat;
								},

								4, {

									if ( (item[0].asSymbol == \lin) || (item[0].asSymbol == \sin), {

										listContent[nLocal] = listContent[nLocal].add([
											Env(envLevels, envTimes, item[0].asSymbol),
											scaleT,
											item[0].asSymbol
										]);

										list[nLocal].valueAction_(0);
										listValues[nLocal] = 0;

									}, {
										var curveTemp = [];
										item.do{
											arg i;
											curveTemp = curveTemp.add(i.asFloat);
										};

										listContent[nLocal] = listContent[nLocal].add([
											Env(envLevels, envTimes, curveTemp),
											scaleT,
											curveTemp
										]);

										list[nLocal].value_(0);
										listValues[nLocal] = 0;
										this.env_(listContent[nLocal][0][0], nLocal);
									});
								},
							)
						};
					}, {nil});
				});



				scaleBox[nLocal] = NumberBox(allCV[nLocal], 50@30)
				// .canFocus_(false)
				.normalColor_(Color.black)
				.typingColor_(Color.new255(76,127,191,255))
				.background_(Color(0,0,0,0))
				.resize_(7)
				.clipLo_(0.001)
				.clipHi_(3600)
				.maxDecimals_(3)
				.value_(allScale[nLocal])
				.action_({
					arg item;
					allScale[nLocal] = item.value;
					allEnvWin[nLocal].scale_(item.value);

					allEnv[nLocal] = allEnvWin[nLocal].env;
					lastPoint[nLocal].clipLo_(item.value);
					lastPoint[nLocal].value_(item.value);
				});


				StaticText(allCV[nLocal], 50@30)
				.resize_(7)
				.string_("Time Scale")
				.font_(Font(\calibri, 12));

				lastPoint[nLocal] = NumberBox(allCV[nLocal], 50@30)
				// .canFocus_(false)
				.normalColor_(Color.black)
				.typingColor_(Color.new255(76,127,191,255))
				.background_(Color(0,0,0,0))
				.resize_(7)
				.clipLo_(allScale[nLocal])
				.clipHi_(3600)
				.maxDecimals_(3)
				.scroll_(false)
				.action_({
					arg item;

					if (item.value > allScale[nLocal], {
						newEnv = Env(allEnv[nLocal].levels++[allEnv[nLocal].levels.reverse[0]],
							allEnv[nLocal].times++[item.value-allScale[nLocal]],
							allEnv[nLocal].curves
						);
					});

					this.env_(newEnv, nLocal);
				});

				StaticText(allCV[nLocal], 50@30)
				.resize_(7)
				.string_("Time Limit")
				.font_(Font(\calibri, 12));


				yLo[nLocal] = NumberBox(allCV[nLocal], 50@30)
				.normalColor_(Color.black)
				.typingColor_(Color.new255(76,127,191,255))
				.background_(Color(0,0,0,0))
				.resize_(7)
				.value_(yLoVal[nLocal])
				.action_({
					yLoVal[nLocal] = yLo[nLocal].value;
					allEnvWin[nLocal].yLo_(yLoVal[nLocal]);
				});

				StaticText(allCV[nLocal], 50@30)
				.resize_(7)
				.string_("Value Low")
				.font_(Font(\calibri, 12));

				yHi[nLocal] = NumberBox(allCV[nLocal], 50@30)
				.normalColor_(Color.black)
				.typingColor_(Color.new255(76,127,191,255))
				.background_(Color(0,0,0,0))
				.resize_(7)
				.value_(yHiVal[nLocal])
				.action_({
					yHiVal[nLocal] = yHi[nLocal].value;
					allEnvWin[nLocal].yHi_(yHiVal[nLocal]);
				});

				StaticText(allCV[nLocal], 50@30)
				.resize_(7)
				.string_("Value High")
				.font_(Font(\calibri, 12));

				xTxt[nLocal] = StaticText(win[nLocal], 110@30)
				.resize_(7)
				.string_("time:  ")
				.font_(Font(\calibri, 12));

				yTxt[nLocal] = StaticText(win[nLocal], 110@30)
				.resize_(7)
				.string_("value: ")
				.font_(Font(\calibri, 12));


				pop[nLocal] = PopUpMenu(win[nLocal], 50@30)
				.resize_(9)
				.items_(["Lin", "Sin", "DIY"])
				.action_({
					arg item;

					switch (item.value,
						0, {allEnvWin[nLocal].curve_(\lin);
							allCurve[nLocal] = \lin
						},
						1, {allEnvWin[nLocal].curve_(\sin);
							allCurve[nLocal] = \sin
						},
						2, {nil}
					);
				});

				if (allCurve[nLocal].isArray, {
					pop[nLocal].value_(2);
				}, {
					if (allCurve[nLocal] == \lin) {pop[nLocal].value_(0)} {pop[nLocal].value_(1)};
				});

				StaticText(win[nLocal], 50@30)
				.resize_(9)
				.string_("Curves")
				.font_(Font(\calibri, 12));

				switchPlay = Button(win[nLocal], 50@30)
				.canFocus_(false)
				.font_(Font(\calibri))
				.resize_(9)
				.states_([
					["OFF", Color.black, Color.grey],
					["Play", Color.black, Color(0,1,0,0.6)],
					["Rec", Color.black, Color(1,0,0,0.3)]
				])
				.value_(allSwitchPlay[nLocal])
				.action_({
					arg item;
					allSwitchPlay[nLocal] = item.value;
				});

				StaticText(win[nLocal], 50@30)
				.resize_(7)
				.string_("Mode")
				.font_(Font(\calibri, 12));

				// this is for SynthWin Load
				if ( (listValues[nLocal] == 0) && (listItems[nLocal] != nil), {
					list[nLocal].valueAction_(0);
				});
			});
		});
	} // end of init

	play {
		allGUI.do{

			arg theGUI, i;

			if (allSwitchPlay[i] == 1,{

				var start = thisThread.seconds;

				playList[i]	= Routine{
					inf.round.do{

						var now;

						now = thisThread.seconds - start;

						if ( now < (allScale[i] - 0.0005), { // an algorithm to minimize difference

							allGUI[i].valueAction_( allEnv[i].at(now) );

						}, {

							// ignore the clock, force the last point
							allGUI[i].valueAction_( allEnv[i].at(allScale[i]) );
							// real clock, can show difference
							now.postln;
							playDone.defer;
							playList[i].stop;
						});

						0.001.wait;
					};
				}.play(AppClock);

			});

			if (allSwitchPlay[i] == 2, {

				var start;
				start = thisThread.seconds;

				recTime[i] = recTime[i].add(0);
				recValue[i] = recValue[i].add(theGUI.value);

				recList[i] = Routine{

					inf.do({

						0.001.wait;

						recValue[i] = recValue[i].add(theGUI.value);

						recTime[i] = recTime[i].add(thisThread.seconds - start);

						recGap[i] = recGap[i].add(recTime[i][recTime[i].size-1] - recTime[i][recTime[i].size-2]);

						this.env_(Env(recValue[i], recGap[i]), i);
					});

				}.play(AppClock);
			});
		};
	} // end of play

	playDoneAction_ {
		arg aFunction;
		playDone = aFunction;
	}

	stop {

		playList.do{
			arg rt;
			rt.stop;
		};

		recList.do{
			arg rt;
			rt.stop;
		}
	}

	env_ {
		arg envelope, id;

		allEnvWin[id].env_(envelope);
		allEnv[id] = allEnvWin[id].env;
		scaleBox[id].valueAction_(allEnvWin[id].scale);

		allScale[id] = allEnvWin[id].scale;
		allCurve[id] = allEnvWin[id].curve;

		if (allCurve[id].isArray, {
			pop[id].value_(2);
		}, {
			if (allCurve[id] == \lin) {pop[id].value_(0)} {pop[id].value_(1)};
		});
	}

	resize_ {
		arg resizeValue;
		button.resize_(resizeValue);
	}
}