import type { RenderedLayout } from "../services/exportService";

function SummarySection({ summary }: { summary: string }) {
  if (!summary.trim()) return null;
  return (
    <section className="mb-4">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-1">Summary</h2>
      <p className="text-sm text-gray-800">{summary}</p>
    </section>
  );
}

function ExperienceSection({ layout }: { layout: RenderedLayout }) {
  const { experience } = layout.content;
  if (experience.length === 0) return null;
  return (
    <section className="mb-4">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-2">
        Experience
      </h2>
      <div className="flex flex-col gap-3">
        {experience.map((item, idx) => (
          <div key={idx}>
            <p className="font-medium text-gray-900">
              {item.title} — {item.company}
            </p>
            <p className="text-xs text-gray-500">
              {item.startDate} – {item.currentRole ? "Present" : item.endDate}
            </p>
            <ul className="list-disc list-inside text-sm text-gray-800 mt-1">
              {item.bullets.map((bullet, bIdx) => (
                <li key={bIdx}>{bullet}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  );
}

function EducationSection({ layout }: { layout: RenderedLayout }) {
  const { education } = layout.content;
  if (education.length === 0) return null;
  return (
    <section className="mb-4">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-2">
        Education
      </h2>
      <div className="flex flex-col gap-2">
        {education.map((item, idx) => (
          <div key={idx}>
            <p className="font-medium text-gray-900">
              {item.credential ? `${item.credential}, ` : ""}
              {item.institution}
            </p>
            {item.fieldOfStudy && <p className="text-sm text-gray-600">{item.fieldOfStudy}</p>}
          </div>
        ))}
      </div>
    </section>
  );
}

function SkillsSection({ skills }: { skills: string[] }) {
  if (skills.length === 0) return null;
  return (
    <section className="mb-4">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-2">Skills</h2>
      <p className="text-sm text-gray-800">{skills.join(" · ")}</p>
    </section>
  );
}

const SECTION_RENDERERS: Record<string, (layout: RenderedLayout) => JSX.Element | null> = {
  summary: (layout) => <SummarySection summary={layout.content.summary} />,
  experience: (layout) => <ExperienceSection layout={layout} />,
  education: (layout) => <EducationSection layout={layout} />,
  skills: (layout) => <SkillsSection skills={layout.content.skills} />,
};

export function PreviewRenderer({ layout }: { layout: RenderedLayout }) {
  const { contactInfo } = layout.content;
  const contactParts = [contactInfo.email, contactInfo.phone, contactInfo.location, contactInfo.links].filter(
    Boolean,
  );

  return (
    <div
      className="bg-white rounded-lg shadow-sm p-8 max-w-2xl mx-auto"
      style={{ fontFamily: layout.layoutDescriptor.fontFamily, borderTop: `4px solid ${layout.layoutDescriptor.accentColor}` }}
    >
      <h1 className="text-2xl font-bold text-gray-900">{contactInfo.fullName}</h1>
      <p className="text-sm text-gray-500 mb-6">{contactParts.join("  |  ")}</p>

      {layout.layoutDescriptor.sectionOrder.map((section) => {
        const renderer = SECTION_RENDERERS[section];
        return renderer ? <div key={section}>{renderer(layout)}</div> : null;
      })}
    </div>
  );
}
