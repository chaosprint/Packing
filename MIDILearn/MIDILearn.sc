MIDILearn {

	var parent, bounds, button, resizeValue;

	var temp, openMIDI, allGUI, focusID, allControl;

	var isKnobOrSlider;

	*new {

		arg parent, bounds;

		^super.newCopyArgs(parent,bounds).initMIDILearn;
	}

	initMIDILearn {
		isKnobOrSlider = {|item|
			(item.asString == "a Slider") || (item.asString == "a Knob");
		};

		MIDIClient.init;
		MIDIIn.connectAll;
		MIDIdef.freeAll;

		allControl = Array.newClear(128);

		button = Button(parent, bounds)
		.font_(Font(\calibri))
		.states_([
			["MIDI LEARN", Color.black, Color.grey],
			["MIDI LEARN", Color.black, Color.green]
		])
		.canFocus_(false)
		.action_({
			var check;

			temp = nil;
			allGUI = [];

			//collect all the gui

			check = { |aParentView|
				aParentView.children.do{ |item|
					if (item.asString == "a CompositeView", {
						check(item)
					}, {
						allGUI = allGUI.add(item);
					});
				}
			};

			check.value(parent.view);

			if (button.value == 1, {
				openMIDI = MIDIdef.cc(\cc, {
					arg value, ccnum;

					defer {
						allGUI.do{ //exam which item is focused
							arg item, count;
							if (item.hasFocus, {
								focusID = count;
							})
						};

						if (isKnobOrSlider.value(allGUI[focusID]), {

							//when focusID changes
							allControl[ccnum] = allGUI[focusID];

							//one object, one cc
							MIDIdef.cc(allGUI[focusID].hash.asSymbol).free;

							MIDIdef.cc(allGUI[focusID].hash.asSymbol, {
								arg value;
								defer {
									allControl[ccnum].valueAction_(
										value.linlin(0,127,0,1)
									);
								}
							}, ccnum).permanent_(true);

							defer {
								button.value_(0);//close MIDILearn
							};

							openMIDI.free;//stop listening


							("The " ++ allGUI[focusID].class.asString ++
								"(ID: " ++ focusID.asString ++ ")" ++
								" is mapped to ").post;
							"cc: ".post;
							ccnum.postln;

						},{
							"Only Knob and Slider can be controlled.".postln;
						});

					};//end of defer

				});
			}, {//if user gives up the MIDI learn
				openMIDI.free;
			});

		});

	}

	resize_ {
		arg resizeValue;
		button.resize_(resizeValue);
	}

}