Packing {
	var numOutChannels, wrap, release, lowCut, ramp, countDown, bus, mix, room, damp;

	var out, fx, fxSynth;

	var bufChan, sound, sounds, pattern, packing;

	var tempBufL, tempBufR, routine, ampRoutine;

	//gui
	var win, load, play, auto, osc, recWait, list, gain,
	packText, packNumBox, upLimit, packSlider,
	rangeText, rangeSlider, slider2D, center, rangeCV, sliderX, sliderY,
	plotCV, plotter, playRangeSlider, playCV, playCenter, playRange,
	fadeIn, fadeOut;

	*new {
		arg numOutChannels = 2, wrap = 10, release = 5, lowCut = 30, ramp = 5, countDown = 3, bus = 0, mix = 0.4, room = 0.6, damp = 0.5;
		^super.newCopyArgs(numOutChannels, wrap, release, lowCut, ramp, countDown, bus, mix, room, damp).initPacking;
	}

	initPacking {

		if (wrap <= 5) { Error("Warp must be larger than 5.").throw };

		countDown.postln;
		if (countDown >= 0) {nil} { Error("Invalid count down number.").throw };

		packing = 3;

		Server.local.waitForBoot({

			fx = Bus.audio(Server.local, numOutChannels);

			SynthDef(\fx, {
				arg inBus, outBus=0, mix, room, damp;
				var sig;
				sig = In.ar(inBus, numOutChannels);
				sig = FreeVerb.ar(sig, mix, room, damp);
				Out.ar(outBus, sig);
			}).load;

			Server.local.sync;


			if (bus == \fx, {
				out = fx;
				fxSynth = Synth(\fx, [\inBus, fx, \mix, mix, \room, room, \damp, damp]);
			}, {
				out = bus;
			});
		});

		win = Window(\Packing, Rect(Window.screenBounds.width-1280/2, Window.screenBounds.height-720/2, 1280, 720) )
		.alwaysOnTop_(true)
		.front;
		win.view.decorator_( FlowLayout(win.bounds, 10@10, 10@10) );
		win.view.minSize_( Size(1280,720) );

		load = Button(win, 100@30)
		.resize_(1)
		.font_(Font("calibri"))
		.string_("Load Samples")
		.action_({
			Server.local.waitForBoot({
				FileDialog({
					arg path;
					var listItemsArray;
					sounds = [];
					path.do({
						arg pathtemp;
						listItemsArray = listItemsArray.add(pathtemp);
						sounds = sounds.add(Buffer.read(Server.local, pathtemp.asSymbol));
					});

					list.items_(listItemsArray);
					list.value_(0);
					sound = sounds[0];

					routine = Routine {
						inf.do{
							if (sound.numChannels.isNil, {nil}, {

								if (sound.numChannels == 1, {
									sound.loadToFloatArray(action: {
										arg array;
										{
											plotter.value_(array);
											plotter.domainSpecs_(
												ControlSpec(0, array.size/Server.local.sampleRate, 'lin', 1, 0, "s"));
											plotter.refresh;
										}.defer;
									});
								},{
									tempBufL = Buffer.readChannel(Server.local, sound.path);
									tempBufR = Buffer.readChannel(Server.local, sound.path);

									tempBufL.loadToFloatArray(action: {
										arg array;
										{
											plotter.value_([array,[0!2]]);
											plotter.domainSpecs_(
												ControlSpec(0, array.size/2/Server.local.sampleRate, 'lin', 1, 0, "s"));
											plotter.refresh;
										}.defer;
									});

									tempBufR.loadToFloatArray(action: {
										arg array;
										{
											plotter.value_([plotter.value[0],array]);
											plotter.domainSpecs_(
												ControlSpec(0, array.size/2/Server.local.sampleRate, 'lin', 1, 0, "s"));
											plotter.refresh;
										}.defer;
									});
								});

								routine.stop;
							});
							0.01.wait;
						};
					}.play;

					Pdefn(\ins, \sampler);
					Pdefn(\packing, 3);
					Pdefn(\semi, Pwhite(0,12,inf) );
					Pdefn(\gain, 1);
					Pdefn(\amp, 1).trace;
					Pdefn(\buf, sound);
					Pdefn(\startPosRatio, 0);
					Pdefn(\endPosRatio, 1);
					Pdefn(\fadeInRatio, 0);
					Pdefn(\fadeOutRatio, 0);

					pattern = Pdef(\main,
						Pbind(
							\instrument, Pdefn(\ins),
							\bufnum, Pdefn(\buf),
							\dur, Pdefn(\packing),
							\panFreq, Pdefn(\packing),
							\semi, Pdefn(\semi),
							\amp, Pdefn(\amp),
							\gain, Pdefn(\gain),
							\pos, Pwhite(-1.000, 1.000, inf),
							\startPosRatio, Pdefn(\startPosRatio),
							\endPosRatio, Pdefn(\endPosRatio),
							\fadeInRatio, Pdefn(\fadeInRatio),
							\fadeOutRatio, Pdefn(\fadeOutRatio),
							\outBus, out,
						);
					);

				}, {nil}, 3);
			});
		});

		auto = Automation(win, 100@30)
		.resize_(1);

		MIDILearn(win, 100@30)
		.resize_(1);

		osc = OSCLearn(win, 100@30).resize_(1);

		play = Button(win, 100@30)
		.resize_(1)
		.font_(Font("calibri"))
		.states_([
			["Play", Color.black, Color.grey],
			["Stop", Color.black, Color.green],
		])
		.action_({
			arg item;
			if (item.value == 1, {

				Server.local.waitForBoot({
					bufChan = sound.numChannels;
					SynthDef(\sampler, {
						arg bufnum=0, semi=0, amp=1, gain=1, panFreq=3, outBus=0, pos,
						fadeInRatio=0, fadeOutRatio=0, startPosRatio=0, endPosRatio=1;
						var sig, leng, startPos, endPos, fadeIn, fadeOut, envLo, envHi;

						startPos = startPosRatio * BufDur.ir(bufnum) * SampleRate.ir;
						leng = (endPosRatio - startPosRatio) * BufDur.ir(bufnum) / semi.midiratio;
						fadeIn = fadeInRatio * leng;
						fadeOut = fadeOutRatio * leng;
						sig = PlayBuf.ar(bufChan, bufnum, semi.midiratio, 1, startPos, doneAction:2) * gain;
						4.do{sig = HPF.ar(sig,lowCut)};
						sig = LeakDC.ar(sig);
						envLo = Env([0,1,1,0],[fadeIn, wrap-release, release], \sin).kr(2, leng > wrap);
						envHi = Env([0,1,1,0],[fadeIn, leng - fadeIn - fadeOut, fadeOut], \sin).kr(2, leng <= wrap);
						sig = sig * (envLo + envHi) * amp * gain;
						if (bufChan == 1) { sig = sig!2 };
						if (numOutChannels == 2, {
							Out.ar(outBus, Balance2.ar(sig[0], sig[1], pos ) );
						},{
							Out.ar(outBus,
								PanAz.ar(numOutChannels, sig[0], pos, 0.5) +
								PanAz.ar(numOutChannels, sig[1], pos + Rand(0.125,1.875), 0.5);
							);
						});
					}, ramp).load;
					Server.local.sync;
					pattern.play;

					ampRoutine = Routine({
						var sv = Server.local;
						inf.do{
							if (sv.numSynths > 1 && (sv.numSynths <5), {
								sv.defaultGroup.set(\amp, sv.numSynths.reciprocal.pow(0.1))
							});

							if (sv.numSynths > 5 && (sv.numSynths <10), {
								sv.defaultGroup.set(\amp, sv.numSynths.reciprocal.pow(0.15))
							});

							if (sv.numSynths > 10 && (sv.numSynths <20), {
								sv.defaultGroup.set(\amp, sv.numSynths.reciprocal.pow(0.2))
							});

							if (sv.numSynths > 30, {
								sv.defaultGroup.set(\amp, sv.numSynths.reciprocal.pow(0.25))
							});

							// [sv.numSynths].postln;
							0.01.wait;
						}
					}).play;

					auto.play;
				});
			}, {
				pattern.stop;
				ampRoutine.stop;
				auto.stop;
			});
		});

		Button(win, 100@30)
		.states_([
			["Record", Color.black, Color.red],
			["Stop Rec", Color.black, Color.green]
		])
		.font_(Font(\calibri))
		.resize_(1)
		.action_({
			arg item;
			if (item.value == 1, {

				if (countDown == 0, {
					Server.local.record;
					play.valueAction_(1);
				}, {
					recWait = Routine{
						countDown.asInt.do{
							arg i;
							("Recording will start in "++(countDown.asInt-i).asString++" second").postln;
							1.wait;
						};
						Server.local.record;
						play.valueAction_(1);
						recWait.stop;
					}.play(AppClock);
				});
			},{
				recWait.stop;
				Server.local.stopRecording;
			});
		});

		CompositeView(win, 457@30);

		StaticText(win, 23@30)
		.resize_(3)
		.string_("Gain:");

		gain = NumberBox(win, 100@30)
		.value_(1)
		.clipLo_(0)
		.minDecimals_(2)
		.maxDecimals_(3)
		.scroll_step_(0.1)
		.shift_scale_(10)
		.normalColor_(Color.black)
		.typingColor_(Color.new255(76,127,191,255))
		.background_(Color(0,0,0,0))
		.resize_(3)
		.action_({
			arg item;
			Pdefn(\gain, item.value);
		});

		list = ListView(win, 1260@80)
		.resize_(2)
		.action_({
			arg lv;

			sound = sounds[lv.value];
			if (sound.numChannels == 1, {
				sound.loadToFloatArray(action: {
					arg array;
					{
						plotter.value_(array);
						plotter.domainSpecs_(
							ControlSpec(0, array.size/Server.local.sampleRate, 'lin', 1, 0, "s") );
						plotter.refresh;
					}.defer;
				});
			},{
				tempBufL = Buffer.readChannel(Server.local, sound.path);
				tempBufR = Buffer.readChannel(Server.local, sound.path);

				tempBufL.loadToFloatArray(action: {
					arg array;
					{
						plotter.value_([array,[0!2]]);
						plotter.domainSpecs_(
							ControlSpec(0, array.size/2/Server.local.sampleRate, 'lin', 1, 0, "s"));
						plotter.refresh;
					}.defer;
				});

				tempBufR.loadToFloatArray(action: {
					arg array;
					{
						plotter.value_([plotter.value[0],array]);
						plotter.domainSpecs_(
							ControlSpec(0, array.size/2/Server.local.sampleRate, 'lin', 1, 0, "s"));
						plotter.refresh;
					}.defer;
				});
			});

			routine = Routine {
				inf.do{
					if (sound.numChannels.isNil, {nil}, {
						bufChan = sound.numChannels;
						SynthDef(\sampler, {
							arg bufnum=0, semi=0, amp=1, gain=1, panFreq=3, outBus=0, pos,
							fadeInRatio=0, fadeOutRatio=0, startPosRatio=0, endPosRatio=1;
							var sig, leng, startPos, endPos, fadeIn, fadeOut, envLo, envHi;

							startPos = startPosRatio * BufDur.ir(bufnum) * SampleRate.ir;
							leng = (endPosRatio - startPosRatio) * BufDur.ir(bufnum) / semi.midiratio;
							fadeIn = fadeInRatio * leng;
							fadeOut = fadeOutRatio * leng;
							sig = PlayBuf.ar(bufChan, bufnum, semi.midiratio, 1, startPos, doneAction:2) * gain;
							4.do{sig = HPF.ar(sig,lowCut)};
							sig = LeakDC.ar(sig);
							envLo = Env([0,1,1,0],[fadeIn, wrap-release, release], \sin).kr(2, leng > wrap);
							envHi = Env([0,1,1,0],[fadeIn, leng - fadeIn - fadeOut, fadeOut], \sin).kr(2, leng <= wrap);
							sig = sig * (envLo + envHi) * amp * gain;
							if (bufChan == 1) { sig = sig!2 };
							if (numOutChannels == 2, {
								Out.ar(outBus, Balance2.ar(sig[0], sig[1], pos ) );
							},{
								Out.ar(outBus,
									PanAz.ar(numOutChannels, sig[0], pos, 0.5) +
									PanAz.ar(numOutChannels, sig[1], pos + Rand(0.125,1.875), 0.5);
								);
							});
						}, ramp).load;
						Pdefn(\ins, \sampler);
						Pdefn(\buf, sound);
						routine.stop;
					});
					0.01.wait;
				};
			}.play;
		});

		packText = StaticText(win, 1079@30)
		.resize_(2)
		.font_( Font("calibri") )
		.string_("Interval (seconds): 3");

		StaticText(win, 61@30)
		.resize_(3)
		.font_( Font("calibri") )
		.string_("Max Interval:");

		upLimit = NumberBox(win, 100@30)
		.value_(3)
		.clipLo_(0.01)
		.normalColor_(Color.black)
		.typingColor_(Color.new255(76,127,191,255))
		.background_(Color(0,0,0,0))
		.resize_(3)
		.action_({
			packSlider.valueAction_(packSlider.value);
		});

		packSlider = Slider(win, 1260@30)
		.value_(1)
		.knobColor_(Color(0,0,0,0))
		.resize_(2)
		.action_({
			arg item;
			packing = item.value.linexp(0,1,0.005,upLimit.value);
			packText.string_("Interval (seconds): "++packing.round(0.001).asString);
			if (sounds != nil, {
				Pdefn(\packing, packing);
				Pdefn(\amp, min(packing.clip(0,1).pow(0.2), (Server.local.numSynths+1).reciprocal.pow(0.2) ) );
			});
		});

		rangeText = StaticText(win, 1260@30)
		.resize_(1)
		.font_(Font("calibri"))
		.string_("Pitch Range (MIDI note number): 60-72");

		rangeCV = CompositeView(win, 1260@160)
		.resize_(5);

		rangeSlider = RangeSlider(rangeCV, 1250@10)
		.canFocus_(false)
		.resize_(2)
		.knobColor_(Color(0,0,0,0))
		.action_({
			arg rsl;
			center = (rsl.hi +  rsl.lo)/2;
			slider2D.x = center;
			slider2D.y = (rsl.range/2).pow(0.25);
			sliderX.value_(center);
			sliderY.value_( (rsl.range/2).pow(0.25) );
			Pdefn(\semi, Pwhite(rsl.lo.linlin(0,1,0,127).round(1)-60, rsl.hi.linlin(0,1,0,127).round(1)-60));
			Pdefn(\amp, min(packing.clip(0,1).pow(0.2), (Server.local.numSynths+1).reciprocal.pow(0.2) ) );

			rangeText.string_("Pitch Range (MIDI note number): " ++
				rsl.lo.linlin(0,1,0,127).round(1).asString ++ "-" ++
				rsl.hi.linlin(0,1,0,127).round(1).asString
			);
		});

		slider2D = Slider2D(rangeCV, Rect(0,10,1250,130) )
		.canFocus_(false)
		.resize_(5)
		.knobColor_(Color(0,0,0,0))
		.action_({
			arg sl;
			center = sl.x;
			rangeSlider.hi_( sl.x + (sl.y.pow(4)/2) );
			rangeSlider.lo_( sl.x - (sl.y.pow(4)/2) );
			sliderX.value_(sl.x);
			sliderY.value_(sl.y);
			Pdefn(\semi, Pwhite(rangeSlider.lo.linlin(0,1,0,127).round(1)-60, rangeSlider.hi.linlin(0,1,0,127).round(1)-60));
			Pdefn(\amp, min(packing.clip(0,1).pow(0.2), (Server.local.numSynths+1).reciprocal.pow(0.2) ) );

			rangeText.string_("Pitch Range (MIDI note number): " ++
				rangeSlider.lo.linlin(0,1,0,127).round(1).asString ++ "-" ++
				rangeSlider.hi.linlin(0,1,0,127).round(1).asString
			);
		});

		sliderY = Slider(rangeCV, Rect(1250, 10, 10, 130) )
		.orientation_(\vertical)
		.knobColor_(Color(0,0,0,0))
		.resize_(6)
		.action_({
			arg item;
			slider2D.activey_(item.value);
		});

		sliderX = Slider(rangeCV, Rect(0, 140, 1250, 10) )
		.knobColor_(Color(0,0,0,0))
		.resize_(8)
		.action_({
			arg item;
			slider2D.activex_(item.value);
		});

		slider2D.activex_(0.5);
		slider2D.activey_(0.5);

		plotCV = CompositeView(win, 1260@190)
		.resize_(8);

		plotter = Plotter("plot", parent: plotCV)
		.value_([[0],[0]])
		.domainSpecs_(ControlSpec(0, 1, 'lin', 1, 0, "s"));

		playCV = CompositeView(win, 1260@40)
		.resize_(8);

		playRangeSlider = RangeSlider(playCV, 1260@30)
		.canFocus_(false)
		.resize_(8)
		.knobColor_(Color(0,0,0,0))
		.action_({
			arg rsl;
			Pdefn(\startPosRatio, rsl.lo);
			Pdefn(\endPosRatio, rsl.hi);
			playRange = rsl.range;
			playCenter.value_( (rsl.hi + rsl.lo)/2 );
		});

		playCenter = Slider(playCV, Rect(0,30,1260,10) )
		.resize_(8)
		.knobColor_(Color(0,0,0,0))
		.action_({
			arg item;
			playRangeSlider.lo_( item.value - (playRange/2) );
			playRangeSlider.hi_( item.value + (playRange/2) );
			Pdefn(\startPosRatio, playRangeSlider.lo);
			Pdefn(\endPosRatio, playRangeSlider.hi);
		});

		playRangeSlider.activeLo_(0);
		playRangeSlider.activeHi_(1);

		fadeIn = NumberBox(win, 100@30)
		.value_(0)
		.scroll_step_(0.01)
		.shift_scale_(10)
		.alt_scale_(0.1)
		.minDecimals_(3)
		.clipLo_(0)
		.clipHi_(1)
		.normalColor_(Color.black)
		.typingColor_(Color.new255(76,127,191,255))
		.background_(Color(0,0,0,0))
		.resize_(7)
		.action_({
			arg item;
			fadeOut.clipHi_(1-item.value);
			Pdefn(\fadeInRatio, item.value);
		});

		StaticText(win, 34@30)
		.resize_(7)
		.font_( Font("calibri") )
		.string_("Fade In");

		fadeOut = NumberBox(win, 100@30)
		.value_(0)
		.scroll_step_(0.01)
		.shift_scale_(10)
		.alt_scale_(0.1)
		.minDecimals_(3)
		.clipLo_(0)
		.clipHi_(1)
		.normalColor_(Color.black)
		.typingColor_(Color.new255(76,127,191,255))
		.background_(Color(0,0,0,0))
		.resize_(7)
		.action_({
			arg item;
			fadeIn.clipHi_(1-item.value);
			Pdefn(\fadeOutRatio, item.value);
		});

		StaticText(win, 49@30)
		.resize_(7)
		.font_( Font("calibri") )
		.string_("Fade Out");
	}
}