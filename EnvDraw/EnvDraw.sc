EnvDraw {

	var parent, bounds;

	var xAll, yAll, xGap, <>yLo=0, <>yHi=1;

	var xTemp, yTemp;

	var envWin, menu, envIn, <value, scaleIn=1, <>resizeValue=1;

	var curveIn=\lin, curveCus;

	var actionFuncIn, mouseOverActionIn;

	// own function
	var envCalc, updateEnv, valueCalc;

	// mouseOver
	var <mousePos;

	var dragCurve;

	*new {

		arg parent, bounds;

		^super.newCopyArgs(parent,bounds).init;
	}

	init {

		xAll=[0,1];
		yAll=[0.5,0.5];
		value=[];
		actionFuncIn = {nil}; //in case user has not given a action_()
		mouseOverActionIn = {nil};
		mousePos = [0,0];

		// caculate the env that can be used as automation
		envCalc = {

			xGap = Array.fill(xAll.size - 1, {
				arg count;
				xAll[count+1] - xAll[count];
			});

			envIn = Env(yAll,xGap, curveIn);
		};

		envCalc.defer;

		updateEnv = {
			envWin.setEnv(envIn);
		};

		valueCalc = {
			value = Array.fill(xAll.size, {arg count; [xAll[count], yAll[count]]});
		};

		//init some gui settings
		envWin = EnvelopeView(parent, bounds)

		.keepHorizontalOrder_(true)

		.grid_(Point(0.1, 0.1))

		.gridOn_(true)

		.drawLines_(true)

		.selectionColor_(Color.red)

		.drawRects_(true)
		.step_(1/10000)

		.thumbSize_(7)
		//for safety forbid drag is an option, but bad for copy

		.style_(1)

		.value_([[0,1],[0.5,0.5]])

		// for moving current point
		.action_({
			arg item;

			xAll = item.value[0];
			yAll = item.value[1];

			//keep the first and last points fixed
			xAll[0]=0;
			xAll[xAll.size-1]=1;

			envCalc.defer;
			updateEnv.defer;
			valueCalc.defer;
			actionFuncIn.defer;

			// mouseOverActionIn.defer;//when move a point, it should be moveOver too!

		})
		.mouseDownAction_({

			arg view, x0, y0, mod, buttonNumber, clickCount;
			var pX, pY, num;

			switch ( buttonNumber,

				0, {
					//left click
					if (mod==524288,{

						pX = x0.linlin(0,envWin.bounds.width,0,1);
						pY = y0.linlin(0,envWin.bounds.height,1,0);

						xAll = xAll.add(pX);
						xAll.sort; // tell the exact place
						num = xAll.atIdentityHash(pX); // tell Y array the order
						yAll = yAll.insert(num, y0.linlin(0,envWin.bounds.height,1,0));

						// calculate the Env expression
						xAll[0]=0;
						xAll[xAll.size-1]=1;

						if (curveIn.isArray) {curveIn = curveIn.insert(num, 0)};

						envCalc.defer;
						updateEnv.defer;
						valueCalc.defer;
						actionFuncIn.defer; //do the real function
					});
				},

				1, {
					if (mod==524288, {

						xAll.do({

							// search from x Array

							arg item, count;

							var bottomMatch = (x0 - 5) <= item.linlin(0,1,0,envWin.bounds.width);

							var topMatch = (x0 + 5) >= item.linlin(0,1,0,envWin.bounds.width);

							var leftMatch = yAll[count].linlin(0,1,envWin.bounds.height,0) >= (y0 - 5);

							var rightMatch = yAll[count].linlin(0,1,envWin.bounds.height,0) <= (y0 + 5);

							var match = bottomMatch && topMatch && leftMatch && rightMatch;

							if ( match, {

								if (yAll.size == 2, {
									"first or last point could not be delete!"
								},{
									// more than 2 points

									if (count != 0, { // do not allow delete point 0, but can del last p

										if (count == (xAll.size-1),{ // if last point, rescale

											scaleIn = this.valueScale.reverse[1][0];


											xAll.removeAt(count);
											yAll.removeAt(count);
											xAll = xAll.normalize;

											if (curveIn.isArray) {curveIn.removeAt(count)};

										},{
											xAll.removeAt(count);
											yAll.removeAt(count);
											if (curveIn.isArray) {curveIn.removeAt(count)};
										});
									});

									envCalc.defer;
									updateEnv.defer;
									// envWin.valueAction_([xAll, yAll]);
									//after deleting the point, show all the points
									valueCalc.defer;
									actionFuncIn.defer; //do the action when delete a point
								});
							}); // end of if

						}); // end of search

					});

					if (mod == 131072, {

						var tempX = [], place, isHigher, isRising;

						pX = x0.linlin(0,envWin.bounds.width,0,1);
						pY = y0.linlin(0,envWin.bounds.height,1,0);

						place = (xAll ++ [pX]).sort.atIdentityHash(pX);

						if (curveIn.isArray) {} {curveIn = 0!xGap.size};

						isHigher = pY > envIn.at(pX);

						isRising = yAll[place-1] < yAll[place];

						if (isRising, {

							if (isHigher, {
								curveIn[place-1] = curveIn[place-1]-1;
							}, {
								curveIn[place-1] = curveIn[place-1]+1;
							});

						}, {

							if (isHigher, {
								curveIn[place-1] = curveIn[place-1]+1;
							}, {
								curveIn[place-1] = curveIn[place-1]-1;
							});
						});

						envCalc.defer;
						updateEnv.defer;
						valueCalc.defer;
						actionFuncIn.defer;
					});

					if (mod == 0) {mousePos.postln};
				} //end of switch 1
			) // end of switch
		}) // end of mouseDown

		.mouseOverAction_({
			arg view, x0, y0;

			var pX, pY;

			pX = x0.linlin(0,envWin.bounds.width,0,1);
			pY = y0.linlin(0,envWin.bounds.height,1,0);

			mousePos = [(pX*scaleIn).round(0.0001), pY.linlin(0,1,yLo,yHi).round(0.0001)];
			mouseOverActionIn.defer;
		})
		.mouseMoveAction_({ // this is a complement of mouseOverAction when you move a point
			arg view, x0, y0;

			var pX, pY;

			pX = x0.linlin(0,envWin.bounds.width,0,1);
			pY = y0.linlin(0,envWin.bounds.height,1,0);

			mousePos = [(pX*scaleIn).round(0.0001), pY.linlin(0,1,yLo,yHi).round(0.0001)];
			mouseOverActionIn.defer;
		})

		.beginDragAction_({
			arg view,x0,y0;
			~myCurveToDrag = [];
			view.value;
		})

		// if first and last x is not 0, do not allow drag;
		.canReceiveDragHandler_({
			arg view,x0,y0;
			var array;

			if (~myCurveToDrag.size < 1, {
				~myCurveToDrag = ~myCurveToDrag.add(this.curve);
			});

			if ( View.currentDrag.isArray, {
				array = View.currentDrag;
				(array[0][0]==0) &&
				(array[0][(array[0].size)-1]==1);
			},{
				false
			});
		})

		.receiveDragHandler_({
			arg view,x0,y0;
			var array;

			array = View.currentDrag;

			xAll = array[0];
			yAll = array[1];

			//the Env UGen has a diffent expression, we need to calculate it

			xGap = Array.fill(xAll.size - 1, {
				arg count;
				xAll[count+1] - xAll[count];
			});

			curveIn = ~myCurveToDrag[0];

			envIn = Env(yAll, xGap, curveIn);

			updateEnv.defer;
			valueCalc.defer;

			~myCurveToDrag = [];

		});

		valueCalc.defer;

	}

	value_ {
		arg anArray;

		anArray.do{
			arg xy;
			if ( ((xy[0]<0) || (xy[1]>1) || (xy[1]<0)), {
				Error("Invalid range.").throw
			})
		};

		if ((anArray[0][0]!=0) || (anArray[anArray.size-1][0]!=1) , {
			Error("First or last point invalid.").throw;
		});

		xAll = [];
		yAll = [];
		xTemp = [];
		yTemp = [];

		anArray.do{
			arg xy;
			xTemp = xTemp.add(xy[0]);
			yTemp = yTemp.add(xy[1]);
		};

		(xTemp.size-1).do{
			arg count;
			if ( ((xTemp[count+1]) < xTemp[count]), {
				Error("Invalid X order.").throw
			})
		};

		xAll = xTemp;
		yAll = yTemp;

		envCalc.defer;
		updateEnv.defer;
		valueCalc.defer;

	}

	valueScale {
		^Array.fill(xAll.size, {arg count; [xAll[count]*scaleIn, yAll[count]]});
	}

	valueScale_ {
		arg anArray;

		anArray.do{
			arg xy;
			if ( ((xy[0]<0) || (xy[1]>1) || (xy[1]<0)), {
				Error("Invalid range.").throw
			})
		};

		if ((anArray[0][0]!=0), {
			Error("First point invalid.").throw;
		});

		xAll = [];
		yAll = [];
		xTemp = [];
		yTemp = [];

		anArray.do{
			arg xy;
			xTemp = xTemp.add(xy[0]);
			yTemp = yTemp.add(xy[1]);
		};

		(xTemp.size-1).do{
			arg count;
			if ( ((xTemp[count+1]) < xTemp[count]), {
				Error("Invalid X order.").throw
			})
		};

		scaleIn = anArray[(anArray.size)-1][0];
		xAll = xTemp/scaleIn;
		yAll = yTemp;

		envCalc.defer;
		updateEnv.defer;
		valueCalc.defer;

	}

	action_ {
		arg aFunction;
		actionFuncIn = aFunction;
	}

	mouseOverAction_ {
		arg aFunction;
		mouseOverActionIn = aFunction;
	}

	scale {
		^scaleIn;
	}

	scale_ {
		arg scale=1;
		scaleIn = scale;
	}

	env {
		^Env(yAll,xGap*scaleIn,envIn.curves)
	}

	env_ {
		arg envelope;

		envIn = envelope;
		envWin.setEnv(envIn);
		curveIn = envelope.curves;
		yAll = envelope.levels;

		xAll = [0];
		envelope.times.do{
			arg gap, count;
			xAll = xAll.add(xAll[count] + gap)
		};

		scaleIn = xAll[xAll.size-1];
		xAll = xAll / scaleIn;
		xGap = envelope.times / scaleIn;
		valueCalc.defer;
	}

	envArray {
		^[yAll,xGap*scaleIn,envIn.curves]
	}

	curve {
		^envIn.curves;
	}

	curve_ { arg curve=\lin;
		curveIn = curve;
		envIn = Env(yAll,xGap,curve);
		envWin.setEnv(envIn);
	}

	resize_ {
		arg resizeValue;
		envWin.resize_(resizeValue);
	}
}