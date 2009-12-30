
var hide = null;
var show = null;
var children = null;

function init() {
  /* Search form initialization */
  var form = document.forms['search'];
  if (form != null) {
    form.elements['domains'].value = location.hostname;
    form.elements['sitesearch'].value = location.hostname;
  }

  /* Children initialization */
  hide = document.getElementById('hide');
  show = document.getElementById('show');
  children = document.all != null ?
             document.all['children'] :
             document.getElementById('children');
  if (children != null) {
    children.style.display = 'none';
    show.style.display = 'inline';
    hide.style.display = 'none';
  }
}

function showChildren() {
  children.style.display = 'block';
  show.style.display = 'none';
  hide.style.display = 'inline';
}

function hideChildren() {
  children.style.display = 'none';
  show.style.display = 'inline';
  hide.style.display = 'none';
}
