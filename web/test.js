

function el(id){
  return document.getElementById(id);
}

ReactNativeComms.addReactNativeEventListener(function(e){
  var newEl = document.createElement("span");
  newEl.innerText = "Message from RN : " + e.message;
  el("messages").appendChild(newEl);
  el("messages").appendChild(document.createElement("br"));
});

el("btn").addEventListener("click", function(){
  ReactNativeComms.postToReactNative({data : "Hello From WebView"});
});
