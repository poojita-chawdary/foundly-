import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
export function Info({ page }: { page: string }) {
  const content: Record<
    string,
    { title: string; intro: string; sections: [string, string][] }
  > = {
    about: {
      title: "A campus that looks out for each other.",
      intro:
        "Foundly is an independent student project that helps people return lost belongings. It is not affiliated with any university.",
      sections: [
        [
          "Why Foundly?",
          "A lost item should not mean endless group messages. Foundly brings reports, useful match suggestions, and private claims into one place.",
        ],
        [
          "Built for your campus",
          "Reports are visible to signed-in members of the same campus. No live statistics or reports are invented.",
        ],
      ],
    },
    help: {
      title: "A little help finding your way.",
      intro: "Three small steps can turn a lost item into a happy reunion.",
      sections: [
        [
          "1. Report it",
          "Choose “I lost something” or “I found something”. Add a general description, category, campus location, and date. A photo is optional.",
        ],
        [
          "2. Search and compare",
          "Use the noticeboard filters or check potential matches in your dashboard. Matching considers category, shared keywords, colour, location, and date. A similarity score is not a probability of ownership.",
        ],
        [
          "3. Claim and return",
          "Submit a private detail that demonstrates ownership. The finder or a campus moderator reviews it. If approved, agree on a public handover in the private claim plan. The finder records the return after handover.",
        ],
        [
          "Need to update something?",
          "Edit or close an active report from its item page. Pending claims can be withdrawn from your dashboard. Report suspicious listings using the form at the bottom of the item page.",
        ],
      ],
    },
    safety: {
      title: "Good reunions start with a little care.",
      intro:
        "Foundly helps you organise a return. Always use your judgement and campus support.",
      sections: [
        [
          "Keep identifying details private",
          "Do not post ID numbers, full contact details, access codes, or distinguishing secrets in a report or image. Share ownership evidence only in the private claim form.",
        ],
        [
          "Verify before handing over",
          "Check details the claimant could not learn from the listing. Foundly does not automatically prove ownership or reveal an answer to claimants.",
        ],
        [
          "Meet in a safe place",
          "Use the campus help desk or another appropriate, busy public location. Do not visit an isolated location. Never pay a recovery fee or share financial information.",
        ],
        [
          "Report suspicious activity",
          "Use “Report this listing” on an item page. Campus moderators can review reports, hide inappropriate listings, and audit decisions. Contact your campus help desk directly for urgent issues.",
        ],
      ],
    },
    contact: {
      title: "Need a hand?",
      intro: "For help returning an item, visit your campus help desk.",
      sections: [
        [
          "Questions about a report",
          "Use the ownership claim flow to share private identifying details with the finder. Do not publish your email or phone number.",
        ],
        [
          "Abuse or suspicious activity",
          "Flag the listing from its item page. For account or membership verification, contact your deployment’s campus administrator.",
        ],
        [
          "About this demonstration",
          "This project does not provide a live support inbox or claim affiliation with a real college.",
        ],
      ],
    },
  };
  const c = content[page];
  return (
    <article className="narrow wide section info-page">
      <div className="eyebrow">THE FOUNDLY GUIDE</div>
      <h1 className="page-title">{c.title}</h1>
      <p className="lead">{c.intro}</p>
      {c.sections.map(([t, d]) => (
        <section className="panel" key={t}>
          <h2>{t}</h2>
          <p>{d}</p>
        </section>
      ))}
      <Link to="/browse" className="button">
        Explore your campus <ArrowRight size={17} />
      </Link>
    </article>
  );
}
