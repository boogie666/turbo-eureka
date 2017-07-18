

function el(id){
  return document.getElementById(id);
}

ReactNativeComms.addReactNativeEventListener(function(e){
  var newEl = document.createElement("span");
  newEl.innerText = "Message from RN : " + JSON.stringify(e);

  el("messages").appendChild(newEl);
  el("messages").appendChild(document.createElement("br"));

  el("box").style.left = Math.round(e.data.x) + "px";
  el("box").style.top = Math.round(e.data.y) + "px";

});

el("btn").addEventListener("click", function(){
  ReactNativeComms.postToReactNative({data : "Hello From WebView"});
});
