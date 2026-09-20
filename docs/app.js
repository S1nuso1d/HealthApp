(function () {
  const menuBtn = document.querySelector(".menu-btn");
  const nav = document.querySelector(".nav");
  if (menuBtn && nav) {
    menuBtn.addEventListener("click", function () {
      const open = nav.classList.toggle("is-open");
      menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
    });
    nav.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        nav.classList.remove("is-open");
        menuBtn.setAttribute("aria-expanded", "false");
      });
    });
  }

  const screens = Array.prototype.slice.call(document.querySelectorAll("[data-screen]"));
  const chips = Array.prototype.slice.call(document.querySelectorAll("[data-demo]"));
  const navButtons = Array.prototype.slice.call(document.querySelectorAll(".nav-bar [data-demo]"));

  function showScreen(name) {
    screens.forEach(function (el) {
      el.classList.toggle("is-on", el.getAttribute("data-screen") === name);
    });
    chips.concat(navButtons).forEach(function (el) {
      el.classList.toggle("is-on", el.getAttribute("data-demo") === name);
      el.classList.toggle("is-active", el.getAttribute("data-demo") === name);
    });
  }

  document.querySelectorAll("[data-demo]").forEach(function (el) {
    el.addEventListener("click", function () {
      showScreen(el.getAttribute("data-demo"));
    });
  });

  const order = ["home", "sleep", "food", "move", "ai"];
  let index = 0;
  const demoRoot = document.getElementById("demo-phone");
  setInterval(function () {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    if (document.hidden) return;
    if (demoRoot && demoRoot.matches(":hover")) return;
    index = (index + 1) % order.length;
    showScreen(order[index]);
  }, 5000);
})();
