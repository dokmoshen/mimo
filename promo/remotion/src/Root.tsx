import { Composition } from "remotion";
import { Promo } from "./Promo";

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="MimoPromo"
      component={Promo}
      durationInFrames={675}
      fps={30}
      width={1080}
      height={1920}
    />
  );
};
