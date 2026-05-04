import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Boxes, Globe2, ShieldCheck } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import LandingNavbar from '../components/LandingNavbar';

const landingFeatures = [
	{
		icon: Boxes,
		titleKey: 'landing.featureCards.inventoryTitle',
		descriptionKey: 'landing.featureCards.inventoryDescription',
	},
	{
		icon: Globe2,
		titleKey: 'landing.featureCards.marketplaceTitle',
		descriptionKey: 'landing.featureCards.marketplaceDescription',
	},
	{
		icon: ShieldCheck,
		titleKey: 'landing.featureCards.operationsTitle',
		descriptionKey: 'landing.featureCards.operationsDescription',
	},
];

export default function Landing(): JSX.Element {
	const { t } = useTranslation();

	useEffect(() => {
		const revealNodes = Array.from(document.querySelectorAll<HTMLElement>('[data-reveal]'));
		const observer = new IntersectionObserver(
			(entries) => {
				entries.forEach((entry) => {
					if (entry.isIntersecting) {
						entry.target.classList.remove('opacity-0', 'translate-y-6');
						entry.target.classList.add('opacity-100', 'translate-y-0');
						observer.unobserve(entry.target);
					}
				});
			},
			{ threshold: 0.18, rootMargin: '0px 0px -30px 0px' },
		);
		revealNodes.forEach((node) => observer.observe(node));

		const tiltNodes = Array.from(document.querySelectorAll<HTMLElement>('[data-tilt]'));
		const cleanups = tiltNodes.map((card) => {
			const onMouseMove = (event: MouseEvent): void => {
				const rect = card.getBoundingClientRect();
				const relativeX = event.clientX - rect.left;
				const relativeY = event.clientY - rect.top;
				const rotateX = ((relativeY - rect.height / 2) / (rect.height / 2)) * -8;
				const rotateY = ((relativeX - rect.width / 2) / (rect.width / 2)) * 8;
				card.style.transform = `perspective(1100px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg)`;
			};

			const onMouseEnter = (): void => {
				card.style.transition = 'transform 120ms linear';
			};

			const onMouseLeave = (): void => {
				card.style.transform = 'perspective(1100px) rotateX(0deg) rotateY(0deg)';
				card.style.transition = 'transform 320ms ease';
			};

			card.addEventListener('mousemove', onMouseMove);
			card.addEventListener('mouseenter', onMouseEnter);
			card.addEventListener('mouseleave', onMouseLeave);

			return () => {
				card.removeEventListener('mousemove', onMouseMove);
				card.removeEventListener('mouseenter', onMouseEnter);
				card.removeEventListener('mouseleave', onMouseLeave);
				card.style.transform = '';
				card.style.transition = '';
			};
		});

		return () => {
			observer.disconnect();
			cleanups.forEach((cleanup) => cleanup());
		};
	}, []);

	return (
		<div className="relative">
			<LandingNavbar />

			<section className="relative overflow-hidden px-4 pb-20 pt-40 sm:px-6 lg:px-8">
				<div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_15%_10%,rgba(16,185,129,0.22),transparent_40%),radial-gradient(circle_at_85%_20%,rgba(14,165,233,0.2),transparent_42%)]" />
				<div className="relative mx-auto flex w-full max-w-7xl flex-col items-start gap-8">
					<p
						data-reveal
						className="rounded-full border border-primary-300/70 bg-primary-100/75 px-4 py-1.5 text-sm font-semibold uppercase tracking-[0.14em] text-primary-700 opacity-0 translate-y-6 transition-all duration-700 dark:border-primary-400/20 dark:bg-primary-500/10 dark:text-primary-200"
					>
						{t('landing.heroEyebrow')}
					</p>
					<h1
						data-reveal
						className="max-w-4xl text-4xl font-black leading-tight tracking-tight text-slate-900 opacity-0 translate-y-6 transition-all duration-700 delay-100 dark:text-white sm:text-5xl lg:text-6xl"
					>
						{t('landing.heroTitle')}
					</h1>
					<p
						data-reveal
						className="max-w-2xl text-lg text-slate-600 opacity-0 translate-y-6 transition-all duration-700 delay-200 dark:text-slate-300"
					>
						{t('landing.heroDescription')}
					</p>
					<div
						data-reveal
						className="flex flex-wrap gap-3 opacity-0 translate-y-6 transition-all duration-700 delay-300"
					>
						<Link to="/register" className="hover-target btn-primary rounded-lg px-6 py-3 text-sm uppercase tracking-wide">
							{t('landing.startTrading')}
						</Link>
						<Link
							to="/marketplace"
							className="hover-target inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-6 py-3 text-sm font-semibold uppercase tracking-wide text-slate-700 transition hover:border-primary-300 hover:text-primary-700 dark:border-white/20 dark:bg-slate-900 dark:text-slate-200"
						>
							{t('landing.exploreMarketplace')}
						</Link>
					</div>
				</div>
			</section>

			<section id="features" className="px-4 py-14 sm:px-6 lg:px-8">
				<div className="mx-auto w-full max-w-7xl space-y-8">
					<div data-reveal className="max-w-2xl opacity-0 translate-y-6 transition-all duration-700">
						<h2 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
							{t('landing.featuresTitle')}
						</h2>
						<p className="mt-2 text-slate-600 dark:text-slate-300">
							{t('landing.featuresDescription')}
						</p>
					</div>

					<div className="grid grid-cols-1 gap-5 md:grid-cols-3">
						{landingFeatures.map((feature, index) => {
							const Icon = feature.icon;
							return (
								<article
									key={feature.titleKey}
									data-reveal
									data-tilt
									className="glass-panel rounded-2xl p-6 opacity-0 translate-y-6 transition-all duration-700 will-change-transform"
									style={{ transitionDelay: `${(index + 1) * 90}ms` }}
								>
									<div className="mb-4 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary-500/15 text-primary-600 dark:text-primary-300">
										<Icon size={22} />
									</div>
									<h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
										{t(feature.titleKey)}
									</h3>
									<p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
										{t(feature.descriptionKey)}
									</p>
								</article>
							);
						})}
					</div>
				</div>
			</section>

			<section className="px-4 pb-14 sm:px-6 lg:px-8">
				<div
					data-reveal
					className="mx-auto flex w-full max-w-7xl flex-col items-start gap-4 rounded-3xl border border-primary-300/40 bg-gradient-to-r from-primary-500/15 to-cyan-400/15 p-8 opacity-0 translate-y-6 transition-all duration-700 dark:border-primary-400/20"
				>
					<h2 className="text-2xl font-bold text-slate-900 dark:text-slate-50">
						{t('landing.ctaTitle')}
					</h2>
					<p className="text-slate-600 dark:text-slate-300">
						{t('landing.ctaDescription')}
					</p>
					<Link to="/register" className="hover-target btn-primary rounded-lg px-6 py-3 text-sm uppercase tracking-wide">
						{t('landing.createAccount')}
					</Link>
				</div>
			</section>

			<footer id="contact" className="border-t border-slate-200/80 px-4 py-10 sm:px-6 lg:px-8 dark:border-white/10">
				<div className="mx-auto flex w-full max-w-7xl flex-col gap-6 text-sm text-slate-500 dark:text-slate-300 md:flex-row md:items-end md:justify-between">
					<div>
						<h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">Drawbridge</h3>
						<p className="mt-1">{t('landing.footerDescription')}</p>
					</div>
					<div className="flex items-center gap-4 font-semibold">
						<Link to="/marketplace" className="hover-target transition hover:text-primary-500">
							{t('navigation.marketplace')}
						</Link>
						<Link to="/login" className="hover-target transition hover:text-primary-500">
							{t('navigation.signIn')}
						</Link>
						<Link to="/register" className="hover-target transition hover:text-primary-500">
							{t('landing.register')}
						</Link>
					</div>
				</div>
			</footer>
		</div>
	);
}
