import React from "react";
import {
  AbsoluteFill,
  Img,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Audio } from "@remotion/media";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { fonts } from "./fonts";

const C = {
  cream: "#F4EEE3",
  ink: "#122019",
  red: "#D71921",
  green: "#2C6A57",
  dark: "#0A0A0A",
  dark2: "#101010",
  lilac: "#DEDCF3",
  yellow: "#F4E9B8",
};

const W = 1080;
const H = 1920;

/* ------------------------------------------------------------------ */
/* background                                                          */
/* ------------------------------------------------------------------ */

const DotGrid: React.FC<{ dark: boolean }> = ({ dark }) => (
  <AbsoluteFill
    style={{
      backgroundImage: `radial-gradient(circle, ${
        dark ? "rgba(255,255,255,0.12)" : "rgba(18,31,25,0.11)"
      } 1.1px, transparent 1.35px)`,
      backgroundSize: "26px 26px",
      opacity: 0.75,
    }}
  />
);

const Particles: React.FC<{ dark: boolean }> = ({ dark }) => {
  const frame = useCurrentFrame();
  const dots = Array.from({ length: 24 }, (_, i) => {
    const depth = 0.3 + ((i * 37) % 100) / 100;
    const x = (i * 137.5 + Math.sin(frame / 42 + i * 1.7) * 24 * depth) % W;
    const y = (i * 241.7 + Math.cos(frame / 37 + i * 1.3) * 17 * depth) % H;
    const red = i % 3 === 0;
    const r = 1.6 + depth * 3.2;
    return { x, y, r, red, depth };
  });
  return (
    <AbsoluteFill>
      {dots.map((d, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: d.x,
            top: d.y,
            width: d.r * 2,
            height: d.r * 2,
            borderRadius: 999,
            background: d.red
              ? `rgba(215,25,33,${0.14 + d.depth * 0.24})`
              : dark
                ? `rgba(244,238,227,${0.08 + d.depth * 0.15})`
                : `rgba(18,31,25,${0.05 + d.depth * 0.12})`,
          }}
        />
      ))}
    </AbsoluteFill>
  );
};

type BGProps = {
  image?: string;
  base: string;
  glow: string;
  dark: boolean;
  opacity?: number;
};

const SceneBG: React.FC<BGProps> = ({ image, base, glow, dark, opacity = 0.17 }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ backgroundColor: base }}>
      {image ? (
        <Img
          src={staticFile(image)}
          style={{
            position: "absolute",
            width: "132%",
            height: "132%",
            left: "-16%",
            top: "-16%",
            objectFit: "cover",
            filter: "blur(55px)",
            opacity,
            transform: `translate(${Math.sin(frame / 90) * 10}px, ${Math.cos(
              frame / 110,
            ) * 8}px) scale(1.1)`,
          }}
        />
      ) : null}
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 18% 16%, ${glow}, transparent 58%)`,
          opacity: 0.9,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 82% 84%, ${glow}, transparent 55%)`,
          opacity: 0.45,
        }}
      />
      <DotGrid dark={dark} />
      <Particles dark={dark} />
      <AbsoluteFill
        style={{
          boxShadow: dark
            ? "inset 0 0 180px 60px rgba(0,0,0,0.6)"
            : "inset 0 0 200px 70px rgba(18,31,25,0.10)",
        }}
      />
    </AbsoluteFill>
  );
};

/* ------------------------------------------------------------------ */
/* 2.5D planes                                                         */
/* ------------------------------------------------------------------ */

type PlaneProps = {
  src: string;
  width: number;
  cx: number;
  cy: number;
  ry: number;
  rx: number;
  delay?: number;
  shadow?: string;
  depth?: number;
};

const ScreenPlane: React.FC<PlaneProps> = ({
  src,
  width,
  cx,
  cy,
  ry,
  rx,
  delay = 0,
  shadow = "0 46px 100px rgba(0,0,0,0.42)",
  depth = 0,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const enter = spring({
    frame: frame - delay,
    fps,
    config: { damping: 200, stiffness: 110 },
    durationInFrames: 26,
  });
  const float = Math.sin(frame / 44) * 6;
  const rotY = ry + Math.sin(frame / 61) * 1.3 + (1 - enter) * 9;
  const rotX = rx + Math.sin(frame / 73) * 0.8 + (1 - enter) * 3;
  return (
    <div
      style={{
        position: "absolute",
        left: cx,
        top: cy + float,
        width,
        transform: `translate(-50%, -50%) translateZ(${depth}px) rotateY(${rotY}deg) rotateX(${rotX}deg) scale(${0.94 + enter * 0.06})`,
        transformStyle: "preserve-3d",
        opacity: 0.15 + enter * 0.85,
      }}
    >
      <Img
        src={staticFile(src)}
        style={{
          display: "block",
          width: "100%",
          borderRadius: 30,
          border: "1px solid rgba(255,255,255,0.28)",
          boxShadow: shadow,
        }}
      />
    </div>
  );
};

const DetailCard: React.FC<PlaneProps> = ({
  src,
  width,
  cx,
  cy,
  ry,
  rx,
  delay = 5,
  shadow = "0 30px 70px rgba(0,0,0,0.38)",
  depth = 130,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const enter = spring({
    frame: frame - delay,
    fps,
    config: { damping: 190, stiffness: 120 },
    durationInFrames: 24,
  });
  const float = Math.sin(frame / 39 + 1.4) * 8;
  const rotY = ry + Math.sin(frame / 55 + 1.2) * 1.8 + (1 - enter) * 11;
  const rotX = rx + Math.sin(frame / 67 + 0.7) * 1.0 + (1 - enter) * 4;
  return (
    <div
      style={{
        position: "absolute",
        left: cx,
        top: cy + float,
        width,
        transform: `translate(-50%, -50%) translateZ(${depth}px) rotateY(${rotY}deg) rotateX(${rotX}deg) scale(${0.92 + enter * 0.08})`,
        transformStyle: "preserve-3d",
        opacity: 0.1 + enter * 0.9,
      }}
    >
      <Img
        src={staticFile(src)}
        style={{
          display: "block",
          width: "100%",
          borderRadius: 24,
          border: "1px solid rgba(255,255,255,0.32)",
          boxShadow: shadow,
        }}
      />
    </div>
  );
};

/* ------------------------------------------------------------------ */
/* captions                                                            */
/* ------------------------------------------------------------------ */

type CaptionProps = {
  title: string[];
  sub: string;
  tone: "ink" | "cream";
  position: "top" | "bottom";
  delay?: number;
};

const Caption: React.FC<CaptionProps> = ({ title, sub, tone, position, delay = 2 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const enter = spring({
    frame: frame - delay,
    fps,
    config: { damping: 200, stiffness: 120 },
    durationInFrames: 20,
  });
  const ink = tone === "ink";
  const kind = ink ? fonts.dot : fonts.display;
  return (
    <div
      style={{
        position: "absolute",
        left: W / 2,
        top: position === "top" ? 92 : undefined,
        bottom: position === "bottom" ? 300 : undefined,
        width: 980,
        transform: `translateX(-50%) translateY(${(1 - enter) * 34}px) perspective(1200px) rotateY(${Math.sin(
          frame / 55,
        ) * 1.2}deg)`,
        opacity: enter,
        borderRadius: 28,
        padding: "30px 38px 32px 66px",
        background: ink ? "rgba(10,10,10,0.94)" : "rgba(244,238,227,0.96)",
        border: ink
          ? "1px solid rgba(255,255,255,0.12)"
          : "1px solid rgba(18,31,25,0.10)",
        boxShadow: ink
          ? "0 34px 70px rgba(0,0,0,0.42)"
          : "0 34px 70px rgba(18,31,25,0.22)",
        color: ink ? C.cream : C.ink,
      }}
    >
      <div
        style={{
          position: "absolute",
          left: 28,
          top: 32,
          width: 9,
          height: 58,
          background: C.red,
        }}
      />
      {title.map((line, i) => (
        <div
          key={i}
          style={{
            fontFamily: kind,
            fontWeight: ink ? 700 : 800,
            fontSize: ink ? 80 : 54,
            lineHeight: ink ? 0.98 : 1.03,
            letterSpacing: ink ? 1.5 : -1.6,
            textTransform: ink ? "uppercase" : "none",
            whiteSpace: "nowrap",
          }}
        >
          {line}
        </div>
      ))}
      <div
        style={{
          marginTop: 14,
          fontFamily: fonts.body,
          fontWeight: 700,
          fontSize: ink ? 26 : 28,
          letterSpacing: 1.6,
          textTransform: "uppercase",
          color: ink ? C.red : "#7A4638",
        }}
      >
        {sub}
      </div>
    </div>
  );
};

/* ------------------------------------------------------------------ */
/* logo / hook / end                                                   */
/* ------------------------------------------------------------------ */

const Logo: React.FC<{ size: number; color?: string }> = ({ size, color = C.ink }) => (
  <svg width={size} height={size} viewBox="0 0 64 64">
    <g stroke={color} strokeWidth={5.5} strokeLinecap="round">
      <path d="M32 12v40M12 32h40M18 18l28 28M46 18 18 46" />
    </g>
    <circle cx={32} cy={32} r={5.5} fill={C.red} />
  </svg>
);

const HookScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const lines = [
    { text: "ТВОИ", color: C.ink },
    { text: "ЗАМЕТКИ", color: C.ink },
    { text: "НЕ ДЛЯ", color: C.red },
    { text: "РЕКЛАМЫ", color: C.red },
  ];
  return (
    <AbsoluteFill>
      <SceneBG base={C.cream} glow="rgba(215,25,33,0.10)" dark={false} />
      <div
        style={{
          position: "absolute",
          left: 66,
          top: 76,
          display: "flex",
          alignItems: "center",
          gap: 20,
        }}
      >
        <Logo size={62} />
        <div
          style={{
            fontFamily: fonts.display,
            fontWeight: 800,
            fontSize: 44,
            letterSpacing: -2,
            color: C.ink,
          }}
        >
          mimo
        </div>
      </div>
      <div style={{ position: "absolute", left: 66, top: 330 }}>
        {lines.map((line, i) => {
          const p = spring({
            frame: frame - i * 3,
            fps,
            config: { damping: 200, stiffness: 120 },
            durationInFrames: 20,
          });
          return (
            <div
              key={line.text}
              style={{
                fontFamily: fonts.display,
                fontWeight: 900,
                fontSize: 94,
                lineHeight: 1.04,
                letterSpacing: -3,
                color: line.color,
                opacity: p,
                translate: `${(1 - p) * -60}px 0px`,
              }}
            >
              {line.text}
            </div>
          );
        })}
      </div>
      <div
        style={{
          position: "absolute",
          left: 68,
          top: 880,
          width: 250,
          height: 8,
          background: C.red,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 68,
          top: 926,
          fontFamily: fonts.body,
          fontWeight: 600,
          fontSize: 35,
          color: "rgba(40,55,47,0.92)",
        }}
      >
        локально · без аккаунта · без облака
      </div>
      <div
        style={{
          position: "absolute",
          left: 68,
          top: 1382,
          fontFamily: fonts.dot,
          fontWeight: 700,
          fontSize: 48,
          color: "rgba(40,55,47,0.82)",
        }}
      >
        mimo — тихие заметки
      </div>
      <div
        style={{
          position: "absolute",
          left: 68,
          top: 1464,
          padding: "18px 28px",
          borderRadius: 40,
          background: C.ink,
          color: C.cream,
          fontFamily: fonts.body,
          fontWeight: 700,
          fontSize: 24,
          letterSpacing: 1.4,
        }}
      >
        ANDROID 9+ · GITHUB / DOKMOSHEN / MIMO
      </div>
      <div style={{ position: "absolute", inset: 0, perspective: 2200 }}>
        <ScreenPlane
          src="images/hook-home.png"
          width={400}
          cx={930}
          cy={1000}
          ry={-16}
          rx={2}
          delay={4}
          shadow="0 44px 90px rgba(18,31,25,0.28)"
        />
      </div>
    </AbsoluteFill>
  );
};

const EndScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const logo = spring({ frame, fps, config: { damping: 180, stiffness: 100 } });
  const text = spring({ frame: frame - 6, fps, config: { damping: 200, stiffness: 120 } });
  return (
    <AbsoluteFill>
      <SceneBG base={C.cream} glow="rgba(215,25,33,0.10)" dark={false} />
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 268,
          display: "flex",
          justifyContent: "center",
          scale: 0.7 + logo * 0.3,
          rotate: `${(1 - logo) * -8}deg`,
        }}
      >
        <Logo size={196} />
      </div>
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 484,
          textAlign: "center",
          fontFamily: fonts.display,
          fontWeight: 900,
          fontSize: 172,
          letterSpacing: -8,
          color: C.ink,
          opacity: text,
          translate: `0px ${(1 - text) * 24}px`,
        }}
      >
        mimo
      </div>
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 716,
          textAlign: "center",
          fontFamily: fonts.display,
          fontWeight: 700,
          fontSize: 31,
          lineHeight: 1.4,
          color: "rgba(40,55,47,0.94)",
          opacity: text,
        }}
      >
        тихие заметки,
        <br />
        которые остаются у вас
      </div>
      <div
        style={{
          position: "absolute",
          left: W / 2 - 72,
          top: 836,
          width: 144,
          height: 7,
          background: C.red,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 906,
          textAlign: "center",
          fontFamily: fonts.body,
          fontWeight: 600,
          fontSize: 32,
          lineHeight: 1.75,
          color: "rgba(40,55,47,0.90)",
          opacity: text,
        }}
      >
        Локально. Без аккаунта. Без облака.
        <br />
        github.com/dokmoshen/mimo
        <br />
        dokmoshen.github.io/mimo
      </div>
      <div
        style={{
          position: "absolute",
          left: W / 2 - 264,
          top: 1450,
          width: 528,
          textAlign: "center",
          padding: "18px 0",
          borderRadius: 42,
          background: C.ink,
          color: C.cream,
          fontFamily: fonts.body,
          fontWeight: 700,
          fontSize: 27,
          letterSpacing: 1.5,
        }}
      >
        ANDROID 9+ · 64-BIT · GPL-3.0
      </div>
    </AbsoluteFill>
  );
};

/* ------------------------------------------------------------------ */
/* app scene                                                           */
/* ------------------------------------------------------------------ */

type AppSpec = {
  bg: BGProps;
  main: PlaneProps;
  detail?: PlaneProps;
  caption: CaptionProps;
};

const AppScene: React.FC<{ spec: AppSpec }> = ({ spec }) => (
  <AbsoluteFill>
    <SceneBG {...spec.bg} />
    <AbsoluteFill style={{ perspective: 2300 }}>
      <ScreenPlane {...spec.main} />
      {spec.detail ? <DetailCard {...spec.detail} /> : null}
    </AbsoluteFill>
    <Caption {...spec.caption} />
  </AbsoluteFill>
);

const homeNothing: AppSpec = {
  bg: {
    image: "images/home-nothing.png",
    base: C.dark,
    glow: "rgba(190,30,30,0.34)",
    dark: true,
    opacity: 0.1,
  },
  main: {
    src: "images/home-nothing.png",
    width: 760,
    cx: 588,
    cy: 965,
    ry: -4,
    rx: 1.5,
    shadow: "0 46px 100px rgba(0,0,0,0.62)",
  },
  detail: {
    src: "images/detail-checklist-top.png",
    width: 470,
    cx: 258,
    cy: 1210,
    ry: 9,
    rx: -2,
  },
  caption: {
    title: ["ЦИФРЫ,", "А НЕ ОБЛАКО"],
    sub: "Nothing-режим",
    tone: "ink",
    position: "top",
  },
};

const menuNothing: AppSpec = {
  bg: {
    image: "images/menu-nothing.png",
    base: "#0B0B0B",
    glow: "rgba(190,30,30,0.28)",
    dark: true,
    opacity: 0.1,
  },
  main: {
    src: "images/menu-nothing.png",
    width: 760,
    cx: 520,
    cy: 960,
    ry: 4,
    rx: -1,
    shadow: "0 46px 100px rgba(0,0,0,0.62)",
  },
  caption: {
    title: ["ДОК → МЕНЮ"],
    sub: "меню и быстрый захват",
    tone: "ink",
    position: "top",
  },
};

const waveScene: AppSpec = {
  bg: {
    image: "images/theme-wave.png",
    base: C.cream,
    glow: "rgba(215,25,33,0.22)",
    dark: false,
    opacity: 0.2,
  },
  main: {
    src: "images/theme-wave.png",
    width: 740,
    cx: 540,
    cy: 960,
    ry: -3,
    rx: 2,
    shadow: "0 44px 90px rgba(18,31,25,0.24)",
  },
  detail: {
    src: "images/detail-theme.png",
    width: 450,
    cx: 320,
    cy: 645,
    ry: 9,
    rx: -2,
  },
  caption: {
    title: ["ДРУГОЙ", "ХАРАКТЕР"],
    sub: "Expressive / Nothing",
    tone: "ink",
    position: "top",
  },
};

const expressiveHome: AppSpec = {
  bg: {
    image: "images/home-expressive.png",
    base: "#E9F0E7",
    glow: "rgba(44,106,87,0.24)",
    dark: false,
    opacity: 0.18,
  },
  main: {
    src: "images/home-expressive.png",
    width: 760,
    cx: 540,
    cy: 980,
    ry: -3.5,
    rx: 2,
    shadow: "0 44px 90px rgba(18,31,25,0.22)",
  },
  detail: {
    src: "images/detail-expressive-card.png",
    width: 380,
    cx: 800,
    cy: 1030,
    ry: -9,
    rx: 2,
  },
  caption: {
    title: ["ЦВЕТ. БУМАГА.", "МЕТКИ."],
    sub: "корешки меток",
    tone: "cream",
    position: "bottom",
  },
};

const readerScene: AppSpec = {
  bg: {
    image: "images/reader.png",
    base: C.lilac,
    glow: "rgba(44,106,87,0.22)",
    dark: false,
    opacity: 0.16,
  },
  main: {
    src: "images/reader.png",
    width: 740,
    cx: 540,
    cy: 960,
    ry: 3,
    rx: -1.5,
    shadow: "0 44px 90px rgba(18,31,25,0.22)",
  },
  caption: {
    title: ["ЧИТАЛКА", "КАК ЖУРНАЛ"],
    sub: "шапка · буквица · прогресс",
    tone: "cream",
    position: "bottom",
  },
};

const checklistScene: AppSpec = {
  bg: {
    image: "images/checklist.png",
    base: C.yellow,
    glow: "rgba(180,120,40,0.22)",
    dark: false,
    opacity: 0.16,
  },
  main: {
    src: "images/checklist.png",
    width: 760,
    cx: 540,
    cy: 980,
    ry: -3,
    rx: 1.5,
    shadow: "0 44px 90px rgba(18,31,25,0.22)",
  },
  detail: {
    src: "images/detail-checklist.png",
    width: 440,
    cx: 820,
    cy: 1120,
    ry: -9,
    rx: 2,
  },
  caption: {
    title: ["ШТАМП.", "ЛИНИЯ. ПРОГРЕСС."],
    sub: "чек-листы с характером",
    tone: "cream",
    position: "bottom",
  },
};

/* ------------------------------------------------------------------ */
/* composition                                                         */
/* ------------------------------------------------------------------ */

export const Promo: React.FC = () => {
  return (
    <AbsoluteFill style={{ backgroundColor: C.cream }}>
      <Audio src={staticFile("promo-audio.mp3")} volume={0.9} />
      <TransitionSeries>
        <TransitionSeries.Sequence name="Hook" durationInFrames={66}>
          <HookScene />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Nothing home" durationInFrames={98}>
          <AppScene spec={homeNothing} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Menu" durationInFrames={129}>
          <AppScene spec={menuNothing} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Theme wave" durationInFrames={67}>
          <AppScene spec={waveScene} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Expressive home" durationInFrames={98}>
          <AppScene spec={expressiveHome} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Reader" durationInFrames={98}>
          <AppScene spec={readerScene} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="Checklist" durationInFrames={82}>
          <AppScene spec={checklistScene} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 8 })}
        />
        <TransitionSeries.Sequence name="End card" durationInFrames={93}>
          <EndScene />
        </TransitionSeries.Sequence>
      </TransitionSeries>
    </AbsoluteFill>
  );
};
