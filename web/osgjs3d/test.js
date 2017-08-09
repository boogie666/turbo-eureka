'use strict';

var OSG = window.OSG;
var osg = OSG.osg;
var osgDB = OSG.osgDB;
var osgViewer = OSG.osgViewer;

window._config = {
    ms: 0.0,
    fps: 0.0,
    loadTime : 0
};

var FPSUpdateCallback = function(config) {
    this._config = config;
};
FPSUpdateCallback.prototype = {
    update: function(node, nv) {
        var currentTime = 1000.0 * nv.getFrameStamp().getDeltaTime();
        var frameNumber = nv.getFrameStamp().getFrameNumber();
        if (frameNumber % 60 === 1) {
            this._config.ms = currentTime;
        } else {
            this._config.ms += (currentTime - this._config.ms) / frameNumber;
        }
        this._config.fps = 1000.0 / currentTime;

        //
        node.traverse(nv);
    }
};

var initDatGUI = function() {
    this._gui = new window.dat.GUI();

    this._gui.add(this._config, 'fps').listen();
    this._gui.add(this._config, 'ms').listen();
    this._gui.add(this._config, 'loadTime').listen();
}.bind(window);

var main = function() {
    // The 3D canvas.
    var canvas = document.getElementById('View');
    var viewer;

    var loadTime = new Date().getTime();
    // The viewer
    viewer = new osgViewer.Viewer(canvas);
    viewer.init();
    var rootNode = new osg.Node();
    viewer.setSceneData(rootNode);
    viewer.setupManipulator();
    viewer.run();

    var fixture = osgDB.readNodeURL('./3d/fixture1.osgjs');
    var option1 = osgDB.readNodeURL('./3d/option1.osgjs');
    var option2 = osgDB.readNodeURL('./3d/option2.osgjs');
    var option3 = osgDB.readNodeURL('./3d/option3.osgjs');
    var option4 = osgDB.readNodeURL('./3d/option4.osgjs');

    function addModel(model){
        var mt = new osg.MatrixTransform();
        osg.mat4.rotateZ(mt.getMatrix(), mt.getMatrix(), -Math.PI);

        mt.addChild(model);

        rootNode.addChild(mt);
    }

    function addModelWithOffset(model, offset){
        var mt = new osg.MatrixTransform();
        osg.mat4.rotateZ(mt.getMatrix(), mt.getMatrix(), -Math.PI + offset/10);

        mt.addChild(model);

        rootNode.addChild(mt);
    }

    Promise.all([fixture, option1, option2, option3, option4]).then(function(models) {
          addModel(models[0]);

          for(var i = 1; i < models.length ; i++){
            var option = models[i];
            for(var j = 0; j < 100 ; j++){
              addModelWithOffset(option, j);
            }
          }

          viewer.getManipulator().computeHomePosition();
          rootNode.addUpdateCallback(new FPSUpdateCallback(window._config));
          window._config.loadTime = new Date().getTime() - loadTime;
    });
    initDatGUI();
};

window.addEventListener('load', main, true);
