const $=s=>document.querySelector(s);
const prompt=$("#prompt"),messages=$("#messages"),welcome=$("#welcome"),toast=$("#toast");
function notify(t){toast.textContent=t;toast.classList.add("show");clearTimeout(window.toastTimer);window.toastTimer=setTimeout(()=>toast.classList.remove("show"),2400)}
function send(text){const value=(text||prompt.value).trim();if(!value)return;welcome.hidden=true;messages.hidden=false;const bubble=document.createElement("div");bubble.className="message";bubble.textContent=value;messages.appendChild(bubble);const note=document.createElement("div");note.className="notice";note.textContent="این فقط پیش‌نمایش رابط کاربری است؛ سرویس هوش مصنوعی هنوز متصل نشده و پاسخی تولید نشده است.";messages.appendChild(note);prompt.value="";messages.scrollTop=messages.scrollHeight}
$("#send").addEventListener("click",()=>send());
prompt.addEventListener("keydown",e=>{if(e.key==="Enter"&&!e.shiftKey){e.preventDefault();send()}});
document.querySelectorAll("[data-prompt]").forEach(b=>b.addEventListener("click",()=>{prompt.value=b.dataset.prompt;send(b.dataset.prompt)}));
document.querySelectorAll(".mode-chip").forEach(b=>b.addEventListener("click",()=>{document.querySelectorAll(".mode-chip").forEach(x=>x.classList.remove("selected"));b.classList.add("selected");notify("حالت انتخاب‌شده: "+b.dataset.mode)}));
$("#newChat").addEventListener("click",()=>{messages.innerHTML="";messages.hidden=true;welcome.hidden=false;prompt.value="";$("#sidebar").classList.remove("open")});
$("#menuToggle").addEventListener("click",()=>$("#sidebar").classList.toggle("open"));
document.querySelectorAll(".nav-item").forEach(b=>b.addEventListener("click",()=>{document.querySelectorAll(".nav-item").forEach(x=>x.classList.remove("active"));b.classList.add("active");$("#sectionTitle").textContent=b.textContent.trim();if(b.dataset.panel!=="chat")notify("بخش "+b.textContent.trim()+" در نقشه محصول قرار دارد و هنوز فعال نشده است.");else notify("صفحه گفت‌وگوی اطلس")})); 
$("#themeToggle").addEventListener("click",()=>document.body.classList.toggle("light"));
let english=false;$("#languageToggle").addEventListener("click",()=>{english=!english;notify(english?"English interface is planned; this preview is currently Persian.":"زبان پیش‌نمایش: فارسی")});
$("#attach").addEventListener?.("click",()=>notify("پیوست فایل در نسخه بعدی فعال می‌شود"));
document.querySelector(".attach").addEventListener("click",()=>notify("پیوست فایل در نسخه بعدی فعال می‌شود"));
