(function(){
	'use strict';
	/*
					 _ _ _ _ _ _
					/|     :   :
		RealWidth  / | HeightOffset
		  \       /  |     :   :
		 __\___/\/___|_ _ _:   :
		|   \ / /:   |         :
		|    \ / :   |     HeightDest
		|   / /  :   |         :
		|___\/___:___|         :
		|   /:---: \- WidthOffset
		|  /   \----- WidthDest:
		| /                    :
		|/_ _ _ _ _ _ _ _ _ _ _:
	*/
	var canvas = document.querySelector('canvas');
	var context = canvas.getContext('2d');
	var SIN60 = Math.sin(Math.PI * 1/3);
	var COS60 = Math.cos(Math.PI * 1/3);
	var TAN60 = Math.tan(Math.PI * 1/3);
	var width = null;
	var height = null;
	var widthDest = null;
	var heightDest = null;
	var widthOffset = null;
	var heightOffset = null;
	var realWidth = null;
	var fontSize = null;
	var measuredSize = null;
	var fontLoaded = false;
	var animationFrameRequest = null;
	var title = 'nicemoe.cn';
	var subtitle = 'Is it alive?';
	var textOffset = [0, 0];
	var dpr = window.devicePixelRatio || 1;

	var requestAnimationFrame = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || window.oRequestAnimationFrame || window.msRequestAnimationFrame || function(callback){
		return setTimeout(callback, 1000 / 60);
	};
	var cancelAnimationFrame = window.cancelAnimationFrame || window.webkitCancelAnimationFrame || window.mozCancelAnimationFrame || window.oCancelAnimationFrame || window.msCancelAnimationFrame || function(id){
		return clearTimeout(id);
	};

	function resizeCanvas(){
		canvas.width = window.innerWidth * dpr;
		canvas.height = window.innerHeight * dpr;
	}

	function calcSize(){
		width = canvas.width;
		height = canvas.height;
		widthDest = height * TAN60;
		heightDest = width / TAN60;
		widthOffset = (width - widthDest) / 2;
		heightOffset = (height - heightDest) / 2;
		realWidth = widthOffset > 0 ? (width - 2 * widthOffset) / SIN60 : width / SIN60;
		fontSize = (width > height ? width : height) * 0.08;
	}

	function clearCanvas(){
		context.clearRect(0, 0, width, height);
	}

	function drawBackground(){
		if (widthOffset > 0) {
			context.beginPath();
			context.fillStyle = '#e699cb';
			context.moveTo(0, 0);
			context.lineTo(0, height);
			context.lineTo(widthOffset + textOffset[0] * 0.2 + textOffset[1] * TAN60 * 0.2, height);
			context.lineTo(widthDest + widthOffset + textOffset[0] * 0.2 + textOffset[1] * TAN60 * 0.2, 0);
			context.fill();
			
			context.beginPath();
			context.fillStyle = '#ffffff';
			context.moveTo(width, 0);
			context.lineTo(width, height);
			context.lineTo(widthOffset + textOffset[0] * 0.2 + textOffset[1] * TAN60 * 0.2, height);
			context.lineTo(widthDest + widthOffset + textOffset[0] * 0.2 + textOffset[1] * TAN60 * 0.2, 0);
			context.fill();
		}
		else {
			context.beginPath();
			context.fillStyle = '#e699cb';
			context.moveTo(0, 0);
			context.lineTo(width, 0);
			context.lineTo(width, heightOffset + textOffset[1] * 0.2 + textOffset[0] / TAN60 * 0.2);
			context.lineTo(0, heightDest + heightOffset + textOffset[1] * 0.2 + textOffset[0] / TAN60 * 0.2);
			context.fill();
			
			context.beginPath();
			context.fillStyle = '#ffffff';
			context.moveTo(0, height);
			context.lineTo(width, height);
			context.lineTo(width, heightOffset + textOffset[1] * 0.2 + textOffset[0] / TAN60 * 0.2);
			context.lineTo(0, heightDest + heightOffset + textOffset[1] * 0.2 + textOffset[0] / TAN60 * 0.2);
			context.fill();
		}
	}
	/*
				   90 deg   /|
				 _____:____/_|
				|\    :   /  |
 XOffsetTitle --|-\   :  /   |
			   \|  \  : /    |
 YOffsetTitle --\---\X:/\    |
				|\  X\/  \---- ^XOffsetLeftExceed
				| \X /\  /   |
	   XOffset -|--\/ /------- ExceedXOffset
				|_\/_/_/_____|
				| /\  /
				|/  \/
	*/

	function drawText(){
		context.save();

		context.translate(textOffset[0], textOffset[1]);
		context.rotate(-Math.PI * 1/6);

		context.font = fontSize + 'px Lemonada';

		var exceedXOffset = (heightOffset < 0 ? height : (height - heightOffset)) * COS60;
		var xOffset = fontSize * 2 / TAN60 - exceedXOffset + fontSize;

		var xOffsetLeftExceed = 2 * (exceedXOffset + xOffset) - fontSize + context.measureText(title).width - realWidth;
		var xOffsetRightExceed = exceedXOffset + xOffset - 1.5 * fontSize + context.measureText(subtitle).width - realWidth;
		//console.log(width, exceedXOffset, widthOffset, SIN60, realWidth, xOffsetLeftExceed, xOffsetRightExceed);
		if (xOffsetLeftExceed < 0 || width > height) {
			xOffsetLeftExceed = 0;
		}
		if (xOffsetRightExceed < 0 || width > height) {
			xOffsetRightExceed = 0;
		}

		var xOffsetTitle = xOffset - xOffsetLeftExceed / 2;
		var xOffsetSubtitle = realWidth - exceedXOffset + xOffset + xOffsetRightExceed / 2;
		var yOffsetTitle = (heightDest + heightOffset) * SIN60 - fontSize * 0.5;
		var yOffsetSubtitle = yOffsetTitle + 1.5 * fontSize;
		if (widthOffset > 0) {
			xOffsetSubtitle += widthOffset / SIN60;
		}

		context.fillStyle = '#ffffff';
		context.textAlign = 'left';
		context.fillText(title, xOffsetTitle, yOffsetTitle);

		context.fillStyle = '#e699cb';
		context.textAlign = 'right';
		context.font = fontSize * 0.75 + 'px Lemonada';
		context.fillText(subtitle, xOffsetSubtitle, yOffsetSubtitle);

		context.restore();
	}

	function draw(){
		resizeCanvas();
		calcSize();
		clearCanvas();
		drawBackground();
		drawText();
		
		if (fontLoaded) return;

		// Times New Roman is about < 160px, and Lemonada is about > 220px
		context.font = '48px Lemonada, "Times New Roman"';
		var curMeasuredSize = context.measureText('nicemoe.cn').width;
		if (!measuredSize) {
			measuredSize = curMeasuredSize;
			animationFrameRequest = requestAnimationFrame(draw);
		}
		else if (measuredSize !== curMeasuredSize || curMeasuredSize > 200) {
			// font has already loaded, stop requestAnimationFrame
			cancelAnimationFrame(animationFrameRequest);
		}
		else {
			animationFrameRequest = requestAnimationFrame(draw);
		}
	}

	context.scale(dpr, dpr);
	window.onresize = draw;
	window.onmousemove = window.ontouchmove = function(event){
		var clientX = (event.clientX ? event.clientX : event.changedTouches ? event.changedTouches[0].clientX : width / 2) * dpr;
		var clientY = (event.clientY ? event.clientY : event.changedTouches ? event.changedTouches[0].clientY : height / 2) * dpr;

		if (clientX < 0) {
			clientX = 0;
		}
		else if (clientX > width) {
			clientX = width;
		}
		
		if (clientY < 0) {
			clientY = 0;
		}
		else if (clientY > height) {
			clientY = height;
		}

		textOffset[0] = Math.asin((clientX * 2 - width) / width) * 2 / Math.PI * fontSize * 0.35;
		textOffset[1] = Math.asin((clientY * 2 - height) / height) * 2 / Math.PI * fontSize * 0.35;

		clearCanvas();
		drawBackground();
		drawText();
	};
	animationFrameRequest = requestAnimationFrame(draw);
})();