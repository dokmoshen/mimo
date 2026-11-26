import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";

await Promise.all([
  loadFont({ family: "Unbounded", url: staticFile("fonts/Unbounded-400.ttf"), weight: "400" }),
  loadFont({ family: "Unbounded", url: staticFile("fonts/Unbounded-600.ttf"), weight: "600" }),
  loadFont({ family: "Unbounded", url: staticFile("fonts/Unbounded-800.ttf"), weight: "800" }),
  loadFont({ family: "Unbounded", url: staticFile("fonts/Unbounded-900.ttf"), weight: "900" }),
  loadFont({ family: "Manrope", url: staticFile("fonts/Manrope-400.ttf"), weight: "400" }),
  loadFont({ family: "Manrope", url: staticFile("fonts/Manrope-600.ttf"), weight: "600" }),
  loadFont({ family: "Manrope", url: staticFile("fonts/Manrope-700.ttf"), weight: "700" }),
  loadFont({ family: "Handjet", url: staticFile("fonts/Handjet-400.ttf"), weight: "400" }),
  loadFont({ family: "Handjet", url: staticFile("fonts/Handjet-600.ttf"), weight: "600" }),
  loadFont({ family: "Handjet", url: staticFile("fonts/Handjet-700.ttf"), weight: "700" }),
]);

export const fonts = {
  display: "Unbounded, sans-serif",
  body: "Manrope, sans-serif",
  dot: "Handjet, sans-serif",
};
