"use client";

import { toPng } from "html-to-image";
import {
  CSSProperties,
  ReactElement,
  ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

const AW = 1080;
const AH = 1920;
const AT7P_W = 1200;
const AT7P_H = 1920;
const AT7L_W = 1920;
const AT7L_H = 1200;
const AT10P_W = 1600;
const AT10P_H = 2560;
const AT10L_W = 2560;
const AT10L_H = 1600;
const FGW = 1024;
const FGH = 500;

const TAB_P_RATIO = 0.667;
const TAB_L_RATIO = 1.5;

const COLORS = {
  primaryLight: "#1B6740",
  secondaryLight: "#4E6357",
  tertiaryLight: "#366B52",
  primaryDark: "#3DDC84",
  secondaryDark: "#A8C5B5",
  tertiaryDark: "#66E8A8",
  ink: "#10281C",
  paper: "#F4F8F5",
  mist: "#DCE9E0",
  deep: "#08140D",
  shell: "#EFF5F1",
};

const ANDROID_SIZES = [{ label: "Phone", w: AW, h: AH }] as const;
const ANDROID_7P_SIZES = [{ label: '7" Portrait', w: AT7P_W, h: AT7P_H }] as const;
const ANDROID_7L_SIZES = [{ label: '7" Landscape', w: AT7L_W, h: AT7L_H }] as const;
const ANDROID_10P_SIZES = [{ label: '10" Portrait', w: AT10P_W, h: AT10P_H }] as const;
const ANDROID_10L_SIZES = [{ label: '10" Landscape', w: AT10L_W, h: AT10L_H }] as const;
const FG_SIZES = [{ label: "Feature Graphic", w: FGW, h: FGH }] as const;

const IMAGE_PATHS = [
  "/app-icon.png",
  "/screenshots/android/phone/en/not-detected.png",
  "/screenshots/android/phone/en/dongle.png",
  "/screenshots/android/phone/en/applied.png",
  "/screenshots/android/tablet-7/portrait/en/main.png",
  "/screenshots/android/tablet-7/landscape/en/main.png",
  "/screenshots/android/tablet-10/portrait/en/main.png",
  "/screenshots/android/tablet-10/landscape/en/main.png",
  "/screenshots/feature-graphic/en/base.png",
] as const;

type Device = "android" | "android-7" | "android-10" | "feature-graphic";
type Orientation = "portrait" | "landscape";
type ImageKey = "not-detected" | "dongle" | "applied" | "main";
type ConfigKey =
  | "android-portrait"
  | "android-7-portrait"
  | "android-7-landscape"
  | "android-10-portrait"
  | "android-10-landscape"
  | "feature-graphic";

type SlideProps = {
  cW: number;
  cH: number;
  image: (key: ImageKey) => string;
  kind: ConfigKey;
};

type SlideDef = {
  id: string;
  component: (props: SlideProps) => ReactElement;
};

type CopyBlock = {
  label: string;
  headline: ReactNode;
  body: string;
};

type Config = {
  key: ConfigKey;
  label: string;
  shortLabel: string;
  device: Device;
  orientation: Orientation;
  cW: number;
  cH: number;
  sizes: readonly { label: string; w: number; h: number }[];
  slides: SlideDef[];
};

const imageCache: Partial<Record<(typeof IMAGE_PATHS)[number], string>> = {};

const copy = {
  hero: {
    label: "KNOBDROID",
    headline: (
      <>
        Fix Apple dongle
        <br />
        volume fast.
      </>
    ),
    body: "Automatic hardware volume repair for the Apple USB-C dongle US version on Android.",
  },
  quiet: {
    label: "OPEN SOURCE",
    headline: (
      <>
        Fully
        <br />
        open source.
      </>
    ),
    body: "Every part of the app is open to inspect, build, and improve. No black box around the fix.",
  },
  detect: {
    label: "NO CATCH",
    headline: (
      <>
        No ads. No trackers.
        <br />
        Free forever.
      </>
    ),
    body: "A single-purpose utility with no monetization layer, no tracking layer, and no paywall around the fix.",
  },
  setup: {
    label: "ONE-TIME SETUP",
    headline: (
      <>
        Turn it on.
        <br />
        Stay ready.
      </>
    ),
    body: "Approve access once, enable the fix, and let the app stay ready for the Apple USB-C dongle US version.",
  },
  apply: {
    label: "INSTANT RESTORE",
    headline: (
      <>
        The fix.
        <br />
        Instantly on.
      </>
    ),
    body: "Apply the single hardware volume fix on demand or let it happen automatically when your Apple USB-C dongle US version connects.",
  },
  trust: {
    label: "BUILT FOR AUDIO",
    headline: (
      <>
        Made for Apple
        <br />
        US dongles.
      </>
    ),
    body: "Focused utility, lightweight interface, and a dedicated workflow for the Apple USB-C dongle US version that Android often mishandles.",
  },
  more: {
    label: "WHY PEOPLE KEEP IT",
    headline: (
      <>
        Small app.
        <br />
        Huge relief.
      </>
    ),
    body: "No guessing, no repeated taps, and no buried mixer controls every time you connect your hardware.",
  },
} as const;

function phoneW(cW: number, cH: number, clamp = 0.84) {
  return Math.min(clamp, 0.72 * (cH / cW) * (9 / 19.5));
}

function tabletPW(cW: number, cH: number, clamp = 0.8) {
  return Math.min(clamp, 0.72 * (cH / cW) * TAB_P_RATIO);
}

function tabletLW(cW: number, cH: number, clamp = 0.62) {
  return Math.min(clamp, 0.75 * (cH / cW) * TAB_L_RATIO);
}

function img(path: (typeof IMAGE_PATHS)[number]) {
  return imageCache[path] || path;
}

async function preloadAllImages() {
  await Promise.all(
    IMAGE_PATHS.map(async (path) => {
      if (imageCache[path]) return;
      const resp = await fetch(path);
      const blob = await resp.blob();
      const dataUrl = await new Promise<string>((resolve) => {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result as string);
        reader.readAsDataURL(blob);
      });
      imageCache[path] = dataUrl;
    }),
  );
}

function captionStyle(cW: number, dark = false): CSSProperties {
  return {
    position: "relative",
    zIndex: 3,
    maxWidth: cW * 0.42,
    color: dark ? "#F6FFF9" : COLORS.ink,
  };
}

function Caption({
  cW,
  label,
  headline,
  body,
  dark = false,
  align = "left",
}: {
  cW: number;
  label: string;
  headline: ReactNode;
  body?: string;
  dark?: boolean;
  align?: "left" | "center";
}) {
  return (
    <div
      style={{
        ...captionStyle(cW, dark),
        textAlign: align,
      }}
    >
      <div
        style={{
          fontSize: cW * 0.024,
          fontWeight: 700,
          letterSpacing: "0.18em",
          opacity: dark ? 0.72 : 0.58,
          marginBottom: cW * 0.02,
        }}
      >
        {label}
      </div>
      <div
        style={{
          fontSize: cW * 0.082,
          fontWeight: 800,
          lineHeight: 0.94,
          letterSpacing: "-0.04em",
        }}
      >
        {headline}
      </div>
      {body ? (
        <div
          style={{
            marginTop: cW * 0.028,
            fontSize: cW * 0.022,
            lineHeight: 1.45,
            maxWidth: cW * 0.31,
            color: dark ? "rgba(246,255,249,0.76)" : "rgba(16,40,28,0.68)",
          }}
        >
          {body}
        </div>
      ) : null}
    </div>
  );
}

function Glow({
  cW,
  size,
  top,
  left,
  color,
  opacity = 0.42,
}: {
  cW: number;
  size: number;
  top: string;
  left: string;
  color: string;
  opacity?: number;
}) {
  return (
    <div
      style={{
        position: "absolute",
        width: cW * size,
        height: cW * size,
        borderRadius: "999px",
        top,
        left,
        background: color,
        filter: "blur(70px)",
        opacity,
      }}
    />
  );
}

function Ring({
  cW,
  size,
  top,
  right,
  color,
}: {
  cW: number;
  size: number;
  top: string;
  right: string;
  color: string;
}) {
  return (
    <div
      style={{
        position: "absolute",
        width: cW * size,
        height: cW * size,
        borderRadius: "999px",
        top,
        right,
        border: `${cW * 0.004}px solid ${color}`,
        opacity: 0.24,
      }}
    />
  );
}

function AndroidPhone({
  src,
  alt,
  style,
}: {
  src: string;
  alt: string;
  style?: React.CSSProperties;
}) {
  return (
    <div style={{ position: "relative", aspectRatio: "9/19.5", ...style }}>
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: "8% / 4%",
          background: "linear-gradient(160deg, #22272b 0%, #0e1214 100%)",
          boxShadow:
            "inset 0 0 0 1px rgba(255,255,255,0.08), 0 24px 70px rgba(3,9,6,0.35)",
          position: "relative",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            position: "absolute",
            top: "1.5%",
            left: "50%",
            transform: "translateX(-50%)",
            width: "3%",
            height: "1.4%",
            borderRadius: "50%",
            background: "#040606",
            border: "1px solid rgba(255,255,255,0.06)",
            zIndex: 20,
          }}
        />
        <div
          style={{
            position: "absolute",
            left: "3.5%",
            top: "2%",
            width: "93%",
            height: "96%",
            borderRadius: "5.5% / 2.6%",
            overflow: "hidden",
            background: "#000",
          }}
        >
          <img
            src={src}
            alt={alt}
            style={{
              display: "block",
              width: "100%",
              height: "100%",
              objectFit: "contain",
              objectPosition: "center",
              background: "#050807",
            }}
            draggable={false}
          />
        </div>
      </div>
    </div>
  );
}

function AndroidTabletP({
  src,
  alt,
  style,
}: {
  src: string;
  alt: string;
  style?: React.CSSProperties;
}) {
  return (
    <div style={{ position: "relative", aspectRatio: "5/8", ...style }}>
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: "4.5% / 2.8%",
          background: "linear-gradient(160deg, #23282c 0%, #0f1316 100%)",
          boxShadow:
            "inset 0 0 0 1px rgba(255,255,255,0.08), 0 26px 82px rgba(3,9,6,0.36)",
          position: "relative",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            position: "absolute",
            top: "1.2%",
            left: "50%",
            transform: "translateX(-50%)",
            width: "1.4%",
            height: "0.88%",
            borderRadius: "50%",
            background: "#040606",
            border: "1px solid rgba(255,255,255,0.07)",
            zIndex: 20,
          }}
        />
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: "4.5% / 2.8%",
            border: "1px solid rgba(255,255,255,0.05)",
            pointerEvents: "none",
            zIndex: 15,
          }}
        />
        <div
          style={{
            position: "absolute",
            left: "3.5%",
            top: "2.2%",
            width: "93%",
            height: "95.6%",
            borderRadius: "2.5% / 1.6%",
            overflow: "hidden",
            background: "#000",
          }}
        >
          <img
            src={src}
            alt={alt}
            style={{
              display: "block",
              width: "100%",
              height: "100%",
              objectFit: "contain",
              objectPosition: "center",
              background: "#050807",
            }}
            draggable={false}
          />
        </div>
      </div>
    </div>
  );
}

function AndroidTabletL({
  src,
  alt,
  style,
}: {
  src: string;
  alt: string;
  style?: React.CSSProperties;
}) {
  return (
    <div style={{ position: "relative", aspectRatio: "8/5", ...style }}>
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: "2.8% / 4.5%",
          background: "linear-gradient(160deg, #23282c 0%, #0f1316 100%)",
          boxShadow:
            "inset 0 0 0 1px rgba(255,255,255,0.08), 0 26px 82px rgba(3,9,6,0.36)",
          position: "relative",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            position: "absolute",
            left: "1.2%",
            top: "50%",
            transform: "translateY(-50%)",
            width: "0.88%",
            height: "1.4%",
            borderRadius: "50%",
            background: "#040606",
            border: "1px solid rgba(255,255,255,0.07)",
            zIndex: 20,
          }}
        />
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: "2.8% / 4.5%",
            border: "1px solid rgba(255,255,255,0.05)",
            pointerEvents: "none",
            zIndex: 15,
          }}
        />
        <div
          style={{
            position: "absolute",
            left: "2.2%",
            top: "3.5%",
            width: "95.6%",
            height: "93%",
            borderRadius: "1.6% / 2.5%",
            overflow: "hidden",
            background: "#000",
          }}
        >
          <img
            src={src}
            alt={alt}
            style={{
              display: "block",
              width: "100%",
              height: "100%",
              objectFit: "contain",
              objectPosition: "center",
              background: "#050807",
            }}
            draggable={false}
          />
        </div>
      </div>
    </div>
  );
}

function PortraitCanvas({
  cW,
  cH,
  children,
  dark = false,
}: {
  cW: number;
  cH: number;
  children: ReactNode;
  dark?: boolean;
}) {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        position: "relative",
        overflow: "hidden",
        background: dark
          ? `linear-gradient(180deg, ${COLORS.deep} 0%, #0E1C14 54%, #163224 100%)`
          : `linear-gradient(180deg, ${COLORS.paper} 0%, #E4F1E8 60%, #D8E8DE 100%)`,
      }}
    >
      {dark ? (
        <>
          <Glow cW={cW} size={0.34} top="-4%" left="66%" color={COLORS.primaryDark} opacity={0.22} />
          <Glow cW={cW} size={0.24} top="78%" left="-6%" color={COLORS.tertiaryDark} opacity={0.18} />
        </>
      ) : (
        <>
          <Glow cW={cW} size={0.32} top="-5%" left="-7%" color={COLORS.primaryDark} opacity={0.16} />
          <Glow cW={cW} size={0.24} top="70%" left="72%" color={COLORS.tertiaryLight} opacity={0.16} />
          <Ring cW={cW} size={0.22} top="9%" right="6%" color="rgba(27,103,64,0.22)" />
        </>
      )}
      {children}
      <div
        style={{
          position: "absolute",
          inset: 0,
          border: `${Math.max(2, cW * 0.002)}px solid ${dark ? "rgba(255,255,255,0.06)" : "rgba(27,103,64,0.05)"}`,
          pointerEvents: "none",
        }}
      />
    </div>
  );
}

function LandscapeCanvas({
  cW,
  cH,
  children,
  dark = false,
}: {
  cW: number;
  cH: number;
  children: ReactNode;
  dark?: boolean;
}) {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        position: "relative",
        overflow: "hidden",
        background: dark
          ? `linear-gradient(135deg, ${COLORS.deep} 0%, #0E1C14 60%, #173426 100%)`
          : `linear-gradient(135deg, ${COLORS.paper} 0%, #EDF5F0 48%, #D9E8DE 100%)`,
      }}
    >
      {dark ? (
        <>
          <Glow cW={cW} size={0.2} top="-8%" left="70%" color={COLORS.primaryDark} opacity={0.18} />
          <Glow cW={cW} size={0.15} top="75%" left="55%" color={COLORS.tertiaryDark} opacity={0.14} />
        </>
      ) : (
        <>
          <Glow cW={cW} size={0.18} top="-8%" left="-2%" color={COLORS.primaryDark} opacity={0.12} />
          <Ring cW={cW} size={0.12} top="12%" right="7%" color="rgba(27,103,64,0.18)" />
        </>
      )}
      {children}
    </div>
  );
}

function renderPortraitDevice(
  Frame: typeof AndroidPhone | typeof AndroidTabletP,
  widthPct: number,
  imageSrc: string,
  alt: string,
  scale = 0.9,
) {
  return (
    <Frame
      src={imageSrc}
      alt={alt}
      style={{
        position: "absolute",
        bottom: "-5%",
        width: `${widthPct * scale * 100}%`,
        left: "50%",
        transform: "translateX(-50%) translateY(16%)",
        zIndex: 4,
      }}
    />
  );
}

function portraitSlides(
  Frame: typeof AndroidPhone | typeof AndroidTabletP,
  widthFn: typeof phoneW | typeof tabletPW,
): SlideDef[] {
  const isPhone = Frame === AndroidPhone;

  return [
    {
      id: "hero",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH}>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8.5%" : "7.5%",
              left: isPhone ? "9%" : "8%",
            }}
          >
            <Caption cW={cW} {...copy.hero} />
          </div>
          {renderPortraitDevice(
            Frame,
            widthFn(cW, cH),
            image("applied"),
            "Volume applied",
            isPhone ? 1 : 0.88,
          )}
        </PortraitCanvas>
      ),
    },
    {
      id: "quiet-dongles",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH} dark>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8%" : "7.5%",
              left: isPhone ? "9%" : "8%",
            }}
          >
            <Caption cW={cW} {...copy.quiet} dark />
          </div>
          {renderPortraitDevice(
            Frame,
            widthFn(cW, cH) * 0.96,
            image("not-detected"),
            "No USB device detected",
            isPhone ? 0.96 : 0.84,
          )}
        </PortraitCanvas>
      ),
    },
    {
      id: "auto-detect",
      component: ({ cW, cH, image }) => {
        const width = widthFn(cW, cH);
        return (
          <PortraitCanvas cW={cW} cH={cH}>
            <div
              style={{
                position: "absolute",
                top: isPhone ? "8.4%" : "7.2%",
                left: isPhone ? "9%" : "8%",
              }}
            >
              <Caption cW={cW} {...copy.detect} />
            </div>
            <Frame
              src={image("not-detected")}
              alt="Waiting for device"
              style={{
                position: "absolute",
                bottom: isPhone ? "-3%" : "-8%",
                left: "-7%",
                width: `${width * (isPhone ? 72 : 60)}%`,
                transform: `${isPhone ? "" : "translateY(10%) "}rotate(-7deg)`,
                opacity: 0.5,
                zIndex: 2,
              }}
            />
            <Frame
              src={image("dongle")}
              alt="Dongle detected"
              style={{
                position: "absolute",
                bottom: isPhone ? "0" : "-5%",
                right: "-6%",
                width: `${width * (isPhone ? 100 : 86)}%`,
                transform: `translateY(${isPhone ? 12 : 16}%)`,
                zIndex: 4,
              }}
            />
          </PortraitCanvas>
        );
      },
    },
    {
      id: "one-time-setup",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH}>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8.6%" : "7.4%",
              right: isPhone ? "9%" : "8%",
              width: isPhone ? "44%" : "36%",
            }}
          >
            <Caption cW={cW} {...copy.setup} align="left" />
          </div>
          <Frame
            src={image("not-detected")}
            alt="Configure default level"
            style={{
              position: "absolute",
              bottom: isPhone ? "-1%" : "-8%",
              left: isPhone ? "2%" : "-2%",
              width: `${widthFn(cW, cH) * (isPhone ? 92 : 76)}%`,
              transform: `translateY(${isPhone ? 12 : 16}%)`,
              zIndex: 4,
            }}
          />
          <div
            style={{
              position: "absolute",
              top: "56%",
              right: "8%",
              zIndex: 5,
              padding: `${cW * 0.018}px ${cW * 0.026}px`,
              borderRadius: cW * 0.022,
              background: "rgba(255,255,255,0.8)",
              backdropFilter: "blur(18px)",
              boxShadow: "0 12px 32px rgba(16,40,28,0.1)",
              fontSize: cW * 0.022,
              fontWeight: 700,
              color: COLORS.primaryLight,
            }}
          >
            One fix enabled
          </div>
        </PortraitCanvas>
      ),
    },
    {
      id: "instant-restore",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH} dark>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8.2%" : "7.4%",
              left: isPhone ? "9%" : "8%",
            }}
          >
            <Caption cW={cW} {...copy.apply} dark />
          </div>
          {renderPortraitDevice(
            Frame,
            widthFn(cW, cH),
            image("applied"),
            "Fix applied",
            isPhone ? 1 : 0.86,
          )}
        </PortraitCanvas>
      ),
    },
    {
      id: "apple-dac-support",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH}>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8.5%" : "7.5%",
              left: isPhone ? "9%" : "8%",
            }}
          >
            <Caption cW={cW} {...copy.trust} />
          </div>
          <Frame
            src={image("dongle")}
            alt="Apple dongle detected"
            style={{
              position: "absolute",
              bottom: isPhone ? "-1%" : "-7%",
              right: isPhone ? "1%" : "-3%",
              width: `${widthFn(cW, cH) * (isPhone ? 94 : 80)}%`,
              transform: `translateY(${isPhone ? 12 : 16}%)`,
              zIndex: 4,
            }}
          />
          <div
            style={{
              position: "absolute",
              left: isPhone ? "9%" : "8%",
              top: isPhone ? "63%" : "50%",
              zIndex: 5,
              display: "flex",
              gap: cW * 0.012,
              flexWrap: "wrap",
              maxWidth: isPhone ? "34%" : "28%",
            }}
          >
          {["US version", "Apple USB-C", "Background ready"].map((pill) => (
            <div
              key={pill}
              style={{
                  padding: `${cW * 0.014}px ${cW * 0.02}px`,
                  borderRadius: 999,
                  border: "1px solid rgba(27,103,64,0.12)",
                  background: "rgba(255,255,255,0.72)",
                  fontSize: cW * 0.018,
                  fontWeight: 600,
                  color: COLORS.secondaryLight,
                }}
              >
                {pill}
              </div>
            ))}
          </div>
        </PortraitCanvas>
      ),
    },
    {
      id: "small-app-huge-relief",
      component: ({ cW, cH, image }) => (
        <PortraitCanvas cW={cW} cH={cH} dark>
          <div
            style={{
              position: "absolute",
              top: isPhone ? "8.2%" : "7.4%",
              left: isPhone ? "9%" : "8%",
            }}
          >
            <Caption cW={cW} {...copy.more} dark />
          </div>
          <Frame
            src={image("applied")}
            alt="Success state"
            style={{
              position: "absolute",
              bottom: isPhone ? "-7%" : "-11%",
              right: isPhone ? "-9%" : "-12%",
              width: `${widthFn(cW, cH) * (isPhone ? 74 : 58)}%`,
              transform: `translateY(${isPhone ? 12 : 16}%) rotate(7deg)`,
              opacity: 0.88,
              zIndex: 3,
            }}
          />
          <div
            style={{
              position: "absolute",
              left: "9%",
              bottom: "10.5%",
              zIndex: 6,
              display: "flex",
              flexWrap: "wrap",
              gap: cW * 0.012,
              maxWidth: "52%",
            }}
          >
            {["Auto trigger"].map(
              (pill) => (
                <div
                  key={pill}
                  style={{
                    padding: `${cW * 0.015}px ${cW * 0.02}px`,
                    borderRadius: 999,
                    background: "rgba(246,255,249,0.1)",
                    border: "1px solid rgba(246,255,249,0.14)",
                    fontSize: cW * 0.019,
                    fontWeight: 600,
                    color: "rgba(246,255,249,0.88)",
                  }}
                >
                  {pill}
                </div>
              ),
            )}
          </div>
        </PortraitCanvas>
      ),
    },
  ];
}

function landscapeSlides(): SlideDef[] {
  const landscapeLayout = (
    copyBlock: CopyBlock,
    imageKey: ImageKey,
    dark = false,
    extra?: ReactNode,
  ): SlideDef["component"] =>
    ({ cW, cH, image }) => (
      <LandscapeCanvas cW={cW} cH={cH} dark={dark}>
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "5.4%",
            width: "36%",
            transform: "translateY(-50%)",
          }}
        >
          <Caption cW={cW} {...copyBlock} dark={dark} />
        </div>
        <AndroidTabletL
          src={image(imageKey)}
          alt={copyBlock.label}
          style={{
            position: "absolute",
            right: "-2.4%",
            top: "50%",
            width: `${tabletLW(cW, cH) * 100}%`,
            transform: "translateY(-50%)",
            zIndex: 4,
          }}
        />
        {extra}
      </LandscapeCanvas>
    );

  return [
    { id: "hero", component: landscapeLayout(copy.hero, "main") },
    { id: "quiet-dongles", component: landscapeLayout(copy.quiet, "main", true) },
    { id: "auto-detect", component: landscapeLayout(copy.detect, "main") },
    { id: "one-time-setup", component: landscapeLayout(copy.setup, "main") },
    { id: "instant-restore", component: landscapeLayout(copy.apply, "main", true) },
    {
      id: "apple-dac-support",
      component: landscapeLayout(
        copy.trust,
        "main",
        false,
        <div
          style={{
            position: "absolute",
            left: "5.4%",
            bottom: "16%",
            display: "flex",
            gap: 12,
            flexWrap: "wrap",
            maxWidth: "34%",
          }}
        >
          {["US version", "Apple USB-C", "Volume auto-fix"].map((pill) => (
            <div
              key={pill}
              style={{
                padding: "10px 16px",
                borderRadius: 999,
                border: "1px solid rgba(27,103,64,0.12)",
                background: "rgba(255,255,255,0.76)",
                fontSize: 24,
                fontWeight: 600,
                color: COLORS.secondaryLight,
              }}
            >
              {pill}
            </div>
          ))}
        </div>,
      ),
    },
    {
      id: "small-app-huge-relief",
      component: landscapeLayout(
        copy.more,
        "main",
        true,
        <div
          style={{
            position: "absolute",
            left: "5.4%",
            bottom: "15%",
            display: "flex",
            flexWrap: "wrap",
            gap: 12,
            maxWidth: "34%",
          }}
        >
          {["Auto trigger", "One-tap fix", "Low friction"].map((pill) => (
            <div
              key={pill}
              style={{
                padding: "10px 16px",
                borderRadius: 999,
                border: "1px solid rgba(246,255,249,0.14)",
                background: "rgba(246,255,249,0.1)",
                fontSize: 24,
                fontWeight: 600,
                color: "rgba(246,255,249,0.88)",
              }}
            >
              {pill}
            </div>
          ))}
        </div>,
      ),
    },
  ];
}

const FG_SLIDE: SlideDef = {
  id: "feature-graphic",
  component: ({ cW, cH }) => (
    <div
      style={{
        width: "100%",
        height: "100%",
        position: "relative",
        overflow: "hidden",
        background: "linear-gradient(135deg, #0A150F 0%, #11251B 48%, #173526 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: `0 ${cW * 0.065}px`,
      }}
    >
      <Glow cW={cW} size={0.22} top="-18%" left="-6%" color={COLORS.primaryDark} opacity={0.16} />
      <Glow cW={cW} size={0.18} top="58%" left="78%" color={COLORS.tertiaryDark} opacity={0.12} />
      <div style={{ display: "flex", alignItems: "center", gap: cW * 0.035, zIndex: 3 }}>
        <img
          src={img("/app-icon.png")}
          alt="KnobDroid app icon"
          style={{
            width: cW * 0.13,
            height: cW * 0.13,
            borderRadius: cW * 0.03,
            boxShadow: "0 16px 40px rgba(0,0,0,0.22)",
          }}
          draggable={false}
        />
        <div>
          <div
            style={{
              fontSize: cW * 0.056,
              fontWeight: 800,
              color: "#F7FFF9",
              lineHeight: 1,
              letterSpacing: "-0.04em",
            }}
          >
            KnobDroid
          </div>
          <div
            style={{
              fontSize: cW * 0.025,
              color: "rgba(247,255,249,0.72)",
              marginTop: cW * 0.008,
              maxWidth: cW * 0.34,
              lineHeight: 1.25,
            }}
          >
            Fix Apple USB-C dongle volume on Android before your music starts.
          </div>
        </div>
      </div>
      <div
        style={{
          position: "relative",
          width: cW * 0.31,
          height: cH * 0.86,
          borderRadius: cW * 0.03,
          background: "linear-gradient(180deg, rgba(61,220,132,0.18), rgba(61,220,132,0.06))",
          boxShadow: "0 28px 60px rgba(0,0,0,0.26)",
          overflow: "hidden",
          transform: "rotate(-8deg)",
        }}
      >
        <img
          src={img("/screenshots/feature-graphic/en/base.png")}
          alt="KnobDroid feature art"
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            objectPosition: "center top",
            opacity: 0.9,
          }}
          draggable={false}
        />
      </div>
    </div>
  ),
};

const CONFIGS: Config[] = [
  {
    key: "android-portrait",
    label: "Android Phone",
    shortLabel: "Phone",
    device: "android",
    orientation: "portrait",
    cW: AW,
    cH: AH,
    sizes: ANDROID_SIZES,
    slides: portraitSlides(AndroidPhone, phoneW),
  },
  {
    key: "android-7-portrait",
    label: 'Android Tablet 7" Portrait',
    shortLabel: '7" Portrait',
    device: "android-7",
    orientation: "portrait",
    cW: AT7P_W,
    cH: AT7P_H,
    sizes: ANDROID_7P_SIZES,
    slides: portraitSlides(AndroidTabletP, tabletPW),
  },
  {
    key: "android-7-landscape",
    label: 'Android Tablet 7" Landscape',
    shortLabel: '7" Landscape',
    device: "android-7",
    orientation: "landscape",
    cW: AT7L_W,
    cH: AT7L_H,
    sizes: ANDROID_7L_SIZES,
    slides: landscapeSlides(),
  },
  {
    key: "android-10-portrait",
    label: 'Android Tablet 10" Portrait',
    shortLabel: '10" Portrait',
    device: "android-10",
    orientation: "portrait",
    cW: AT10P_W,
    cH: AT10P_H,
    sizes: ANDROID_10P_SIZES,
    slides: portraitSlides(AndroidTabletP, tabletPW),
  },
  {
    key: "android-10-landscape",
    label: 'Android Tablet 10" Landscape',
    shortLabel: '10" Landscape',
    device: "android-10",
    orientation: "landscape",
    cW: AT10L_W,
    cH: AT10L_H,
    sizes: ANDROID_10L_SIZES,
    slides: landscapeSlides(),
  },
  {
    key: "feature-graphic",
    label: "Feature Graphic",
    shortLabel: "Feature Graphic",
    device: "feature-graphic",
    orientation: "landscape",
    cW: FGW,
    cH: FGH,
    sizes: FG_SIZES,
    slides: [FG_SLIDE],
  },
];

function imageForConfig(kind: ConfigKey, key: ImageKey) {
  if (kind === "android-portrait") {
    const map = {
      "not-detected": "/screenshots/android/phone/en/not-detected.png",
      dongle: "/screenshots/android/phone/en/dongle.png",
      applied: "/screenshots/android/phone/en/applied.png",
      main: "/screenshots/android/phone/en/applied.png",
    } as const;
    return img(map[key]);
  }

  if (kind === "android-7-portrait") {
    return img("/screenshots/android/tablet-7/portrait/en/main.png");
  }

  if (kind === "android-7-landscape") {
    return img("/screenshots/android/tablet-7/landscape/en/main.png");
  }

  if (kind === "android-10-portrait") {
    return img("/screenshots/android/tablet-10/portrait/en/main.png");
  }

  return img("/screenshots/android/tablet-10/landscape/en/main.png");
}

function ScreenshotPreview({
  cW,
  cH,
  children,
}: {
  cW: number;
  cH: number;
  children: ReactNode;
}) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const [scale, setScale] = useState(1);

  useEffect(() => {
    const el = hostRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => {
      const width = entry.contentRect.width;
      const height = entry.contentRect.height;
      setScale(Math.min(width / cW, height / cH));
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, [cW, cH]);

  return (
    <div
      ref={hostRef}
      style={{
        position: "relative",
        width: "100%",
        aspectRatio: `${cW}/${cH}`,
        background: "#D7E4DB",
        borderRadius: 24,
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "absolute",
          left: "50%",
          top: "50%",
          width: cW,
          height: cH,
          transform: `translate(-50%, -50%) scale(${scale})`,
          transformOrigin: "center center",
        }}
      >
        {children}
      </div>
    </div>
  );
}

async function captureSlide(el: HTMLElement, w: number, h: number): Promise<string> {
  const previousLeft = el.style.left;
  const previousOpacity = el.style.opacity;
  const previousZ = el.style.zIndex;
  el.style.left = "0px";
  el.style.opacity = "1";
  el.style.zIndex = "-1";

  const opts = {
    width: w,
    height: h,
    pixelRatio: 1,
    cacheBust: true,
    backgroundColor: "#ffffff",
  };

  await toPng(el, opts);
  const dataUrl = await toPng(el, opts);

  el.style.left = previousLeft;
  el.style.opacity = previousOpacity;
  el.style.zIndex = previousZ;
  return dataUrl;
}

function downloadDataUrl(dataUrl: string, name: string) {
  const link = document.createElement("a");
  link.href = dataUrl;
  link.download = name;
  link.click();
}

export default function Home() {
  const [ready, setReady] = useState(false);
  const [activeKey, setActiveKey] = useState<ConfigKey>("android-portrait");
  const [exporting, setExporting] = useState<string | null>(null);
  const exportRefs = useRef<Record<string, HTMLDivElement | null>>({});

  useEffect(() => {
    preloadAllImages().then(() => setReady(true));
  }, []);

  const activeConfig = useMemo(
    () => CONFIGS.find((config) => config.key === activeKey) ?? CONFIGS[0],
    [activeKey],
  );

  const exportCurrent = async () => {
    const size = activeConfig.sizes[0];
    for (let i = 0; i < activeConfig.slides.length; i += 1) {
      const slide = activeConfig.slides[i];
      const refKey = `${activeConfig.key}:${slide.id}`;
      const el = exportRefs.current[refKey];
      if (!el) continue;
      setExporting(`${activeConfig.shortLabel} ${i + 1}/${activeConfig.slides.length}`);
      const dataUrl = await captureSlide(el, size.w, size.h);
      downloadDataUrl(
        dataUrl,
        `${String(i + 1).padStart(2, "0")}-${slide.id}-${activeConfig.key}-${size.w}x${size.h}.png`,
      );
      await new Promise((resolve) => setTimeout(resolve, 300));
    }
    setExporting(null);
  };

  const exportAllSets = async () => {
    for (const config of CONFIGS) {
      const size = config.sizes[0];
      for (let i = 0; i < config.slides.length; i += 1) {
        const slide = config.slides[i];
        const refKey = `${config.key}:${slide.id}`;
        const el = exportRefs.current[refKey];
        if (!el) continue;
        setExporting(`${config.shortLabel} ${i + 1}/${config.slides.length}`);
        const dataUrl = await captureSlide(el, size.w, size.h);
        downloadDataUrl(
          dataUrl,
          `${String(i + 1).padStart(2, "0")}-${slide.id}-${config.key}-${size.w}x${size.h}.png`,
        );
        await new Promise((resolve) => setTimeout(resolve, 300));
      }
    }
    setExporting(null);
  };

  if (!ready) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "grid",
          placeItems: "center",
          background: COLORS.paper,
          color: COLORS.ink,
        }}
      >
        Loading screenshot assets...
      </div>
    );
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#F1F5F2",
        position: "relative",
        overflowX: "hidden",
      }}
    >
      <div
        style={{
          position: "sticky",
          top: 0,
          zIndex: 50,
          background: "rgba(255,255,255,0.92)",
          borderBottom: "1px solid #D6E3DA",
          display: "flex",
          alignItems: "stretch",
          backdropFilter: "blur(18px)",
        }}
      >
        <div
          style={{
            flex: 1,
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "12px 18px",
            overflowX: "auto",
            minWidth: 0,
          }}
        >
          <span style={{ fontWeight: 800, fontSize: 14, whiteSpace: "nowrap" }}>
            KnobDroid · Play Store Screenshots
          </span>
          {CONFIGS.map((config) => (
            <button
              key={config.key}
              onClick={() => setActiveKey(config.key)}
              style={{
                padding: "8px 14px",
                borderRadius: 999,
                border: config.key === activeKey ? "1px solid transparent" : "1px solid #D6E3DA",
                background:
                  config.key === activeKey
                    ? "linear-gradient(135deg, #1B6740 0%, #3DDC84 100%)"
                    : "rgba(255,255,255,0.72)",
                color: config.key === activeKey ? "#F8FFFB" : COLORS.secondaryLight,
                cursor: "pointer",
                fontSize: 12,
                fontWeight: 700,
                whiteSpace: "nowrap",
              }}
            >
              {config.shortLabel}
            </button>
          ))}
        </div>
        <div
          style={{
            flexShrink: 0,
            padding: "12px 18px",
            borderLeft: "1px solid #D6E3DA",
            display: "flex",
            gap: 10,
            alignItems: "center",
          }}
        >
          <button
            onClick={exportCurrent}
            disabled={!!exporting}
            style={{
              padding: "9px 16px",
              background: exporting ? "#A8C5B5" : COLORS.primaryLight,
              color: "white",
              border: "none",
              borderRadius: 10,
              fontSize: 12,
              fontWeight: 700,
              cursor: exporting ? "default" : "pointer",
              whiteSpace: "nowrap",
            }}
          >
            {exporting ? `Exporting ${exporting}` : "Export Visible Set"}
          </button>
          <button
            onClick={exportAllSets}
            disabled={!!exporting}
            style={{
              padding: "9px 16px",
              background: "white",
              color: COLORS.primaryLight,
              border: "1px solid #B9CEC0",
              borderRadius: 10,
              fontSize: 12,
              fontWeight: 700,
              cursor: exporting ? "default" : "pointer",
              whiteSpace: "nowrap",
            }}
          >
            Export Every Set
          </button>
        </div>
      </div>

      <div style={{ padding: "22px 24px 30px" }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
            gap: 18,
            alignItems: "start",
          }}
        >
          {activeConfig.slides.map((slide, index) => (
            <div
              key={`${activeConfig.key}:${slide.id}`}
              style={{
                background: "rgba(255,255,255,0.7)",
                border: "1px solid rgba(27,103,64,0.08)",
                borderRadius: 28,
                padding: 14,
                boxShadow: "0 10px 30px rgba(17,37,27,0.06)",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 10,
                  gap: 10,
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 800, color: COLORS.secondaryLight }}>
                  {String(index + 1).padStart(2, "0")} · {slide.id.replaceAll("-", " ")}
                </div>
                <div style={{ fontSize: 11, color: "rgba(16,40,28,0.6)" }}>{activeConfig.label}</div>
              </div>
              <ScreenshotPreview cW={activeConfig.cW} cH={activeConfig.cH}>
                {slide.component({
                  cW: activeConfig.cW,
                  cH: activeConfig.cH,
                  kind: activeConfig.key,
                  image: (key) => imageForConfig(activeConfig.key, key),
                })}
              </ScreenshotPreview>
            </div>
          ))}
        </div>
      </div>

      <div style={{ position: "absolute", left: -9999, top: 0 }}>
        {CONFIGS.map((config) =>
          config.slides.map((slide) => (
            <div
              key={`${config.key}:${slide.id}:export`}
              ref={(node) => {
                exportRefs.current[`${config.key}:${slide.id}`] = node;
              }}
              style={{
                position: "absolute",
                left: -9999,
                top: 0,
                width: config.cW,
                height: config.cH,
              }}
            >
              {slide.component({
                cW: config.cW,
                cH: config.cH,
                kind: config.key,
                image: (key) => imageForConfig(config.key, key),
              })}
            </div>
          )),
        )}
      </div>
    </div>
  );
}
