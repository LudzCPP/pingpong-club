import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, CalendarPlus, Mic, Calendar, Mail, X, ChevronRight } from 'lucide-react';

const COACH_STEPS = [
  {
    icon: Users,
    title: 'Dodaj zawodnika',
    desc: 'Przejdź do zakładki Zawodnicy i dodaj pierwszego zawodnika — możesz stworzyć konto wirtualne bez adresu email, a zaprosić go później.',
    action: { label: 'Przejdź do Zawodników', path: '/players' },
  },
  {
    icon: CalendarPlus,
    title: 'Zaplanuj trening',
    desc: 'W zakładce Treningi kliknij „Nowy trening". Możesz też skorzystać z AI — opisz trening głosem lub tekstem, a system wypełni formularz automatycznie.',
    action: { label: 'Przejdź do Treningów', path: '/trainings' },
  },
  {
    icon: Mic,
    title: 'Wypróbuj AI parsowanie',
    desc: 'W formularzu nowego treningu kliknij ikonę mikrofonu i powiedz np. „trening z Anną jutro o 16, godzina, 120 zł" — system sam uzupełni pola.',
    action: null,
  },
];

const PLAYER_STEPS = [
  {
    icon: Calendar,
    title: 'Sprawdź swój plan',
    desc: 'W zakładce Treningi znajdziesz wszystkie zaplanowane zajęcia. Możesz też otworzyć Kalendarz, żeby zobaczyć plan w widoku miesięcznym.',
    action: { label: 'Przejdź do Treningów', path: '/trainings' },
  },
  {
    icon: Mail,
    title: 'Zaproszenia od trenerów',
    desc: 'Jeśli trener wyśle Ci zaproszenie do swojego zespołu, znajdziesz je w zakładce Zaproszenia. Możesz je zaakceptować lub odrzucić.',
    action: { label: 'Przejdź do Zaproszeń', path: '/invitations' },
  },
];

export default function OnboardingModal({ role, onClose }) {
  const [step, setStep] = useState(0);
  const navigate = useNavigate();

  const steps = role === 'COACH' || role === 'ADMIN' ? COACH_STEPS : PLAYER_STEPS;
  const current = steps[step];
  const isLast = step === steps.length - 1;
  const Icon = current.icon;

  function handleAction() {
    if (current.action) {
      onClose();
      navigate(current.action.path);
    }
  }

  function handleNext() {
    if (isLast) {
      onClose();
    } else {
      setStep(s => s + 1);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-surface border border-border rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">

        {/* Header */}
        <div className="bg-accent/10 border-b border-border/50 px-6 py-5 flex items-center justify-between">
          <div>
            <p className="text-accent text-xs font-semibold tracking-widest uppercase">Witaj w TTManager</p>
            <h2 className="text-white font-bold text-lg mt-0.5">Pierwsze kroki</h2>
          </div>
          <button onClick={onClose} className="text-muted hover:text-white transition-colors p-1">
            <X size={18} />
          </button>
        </div>

        {/* Step indicators */}
        <div className="flex gap-1.5 px-6 pt-5">
          {steps.map((_, i) => (
            <div
              key={i}
              className={`h-1 flex-1 rounded-full transition-colors ${i <= step ? 'bg-accent' : 'bg-border'}`}
            />
          ))}
        </div>

        {/* Content */}
        <div className="px-6 py-6">
          <div className="flex items-start gap-4">
            <div className="bg-accent/15 border border-accent/30 rounded-xl p-3 shrink-0">
              <Icon size={22} className="text-accent" />
            </div>
            <div>
              <h3 className="text-white font-semibold text-base">{current.title}</h3>
              <p className="text-muted text-sm mt-1.5 leading-relaxed">{current.desc}</p>
            </div>
          </div>

          {current.action && (
            <button
              onClick={handleAction}
              className="mt-4 w-full text-sm text-accent hover:text-green-400 border border-accent/30 hover:border-accent/60 rounded-lg py-2.5 transition-colors"
            >
              {current.action.label}
            </button>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 flex items-center justify-between">
          <span className="text-muted text-xs">{step + 1} / {steps.length}</span>
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="text-sm text-muted hover:text-white transition-colors px-3 py-2"
            >
              Pomiń
            </button>
            <button
              onClick={handleNext}
              className="flex items-center gap-1.5 bg-accent hover:bg-green-400 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
            >
              {isLast ? 'Zaczynamy!' : 'Dalej'}
              {!isLast && <ChevronRight size={15} />}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
