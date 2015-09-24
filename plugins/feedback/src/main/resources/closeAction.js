

$("#atlwdg-frame").contents().find("#jic-collector-form").submit(function( event ) {
  console.log("working");
  //closeTrigger.hidePanel();
});




MutationObserver = window.MutationObserver || window.WebKitMutationObserver;

var active = false;
var observer = new MutationObserver(function(mutations, observer) {
// fired when a mutation occurs
//console.log(mutations, observer);
if($("#atlwdg-frame").length) {
  active=true;
} else {
  if(active){
    closeTrigger.hidePanel();
  }
}
});

observer.observe(document, {
subtree: true,
attributes: true
});