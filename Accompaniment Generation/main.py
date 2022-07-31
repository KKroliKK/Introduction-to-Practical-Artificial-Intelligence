from mido import MidiFile, MidiTrack, Message
import numpy as np
from numpy.random import randint, rand
import math
from math import floor
from tqdm import tqdm
import music21
from music21.stream.base import Score


"""Define Key"""

def generate_major_keys() -> list:
    """Generates major keys table"""

    keys = []
    offset = [0, 2, 4, 5, 7, 9, 11]
    for i in range(12):
        keys.append([(i + step) % 12 for step in offset])
    return keys


def generate_minor_keys() -> list:
    """Generates minor keys table"""

    tonalities = []
    offset = [0, 2, 3, 5, 7, 8, 10]
    for i in range(12):
        tonalities.append([(i + step) % 12 for step in offset])
    return tonalities


def define_key(file: Score) -> list:
    """Returns key for the given file"""

    keys_table = {'major': generate_major_keys(),
                  'minor': generate_minor_keys()}
    key = file.analyze('key')
    return keys_table[key.mode][key.tonic.midi % 12]


"""Chords Generation"""

def get_num_of_chords(file: Score) -> int:
    """Returns number of chords to generate in given file"""

    sum = 0
    for obj in file.recurse():
        if type(obj) is music21.stream.base.Measure:
            sum += 1
    return sum * 4


def get_notes_on_quaters(file: Score) -> list:
    """
    This method parses given file and generates list of notes
    or rests on each quarter.

    It is needed becuase we need to generate chords on each quarter.
    Chord to genererate depends on the key and note to accompany.

    returns: list of integers (notes in midi notation) or NONEs (rests)
    """

    parsed = [None] * get_num_of_chords(file)
    sum = 0
    c = 0
    for obj in file.recurse():
        if type(obj) is music21.note.Rest or type(obj) is music21.note.Note:
            duration = obj.duration.quarterLength
            iter = math.ceil(duration)
            duration /= iter

            for _ in range(iter):

                if sum == floor(sum):

                    if type(obj) is music21.note.Rest:
                        parsed[int(sum)] = None

                    if type(obj) is music21.note.Note:
                        parsed[int(sum)] = obj.pitch.midi

                sum += duration

    return parsed


def fill_rests(notes: list) -> list:
    """
    I want to generate chords on rests.
    Chord depends on the note to play with.
    In music we play chords on rests similar to the
    chords played in adjacent notes in the same beat.

    This method fills None values with adjacent 
    notes in the same beat or last note of the left beat.
    Beat-long pauses are not filled.

    param notes: notes to play each quarter
    returns: changed notes list
    """

    notes = notes[:]

    for i in range(len(notes)):
        if notes[i] is None:
            if i % 4 == 0:
                notes[i] = notes[i + 1]
            else:
                notes[i] = notes[i - 1]

    return notes


"""Genetic Algorithm"""

def get_triads(note: int, key: list):
    """
    This method is needed for fitness function.
    It generates triads for the given note in given key.

    Such chords sound pleasent. We can measure similarity of
    generated chords with these ones in fitness function.

    returns: list of triads
    """

    key = key.copy()
    key = key + key

    triads = []
    for i in range(7):
        triad = [key[i], key[i + 2], key[i + 4]]
        if note in triad:
            triads.append(triad)

    return triads


def get_fitness(note: int, key: list, chord: list) -> tuple((int, list)):
    """Fitness function
    It defines fitness score for the given chord

    param note: note to which chord is generated
    param key: key of the song
    param: chord: generated chord to score

    returns: tuple (score, chord)
    """

    score = 1

    # Key correlation
    for n in chord:
        if n % 12 in key:
            score *= 3

    # Chord is better when it includes corresponding note
    for n in chord:
        if n % 12 == note % 12:
            score *= 2

    # Triads based on key and note sound good
    if score == 54:
        chord_ = set([n % 12 for n in chord])
        triads = get_triads(note % 12, key)
        intersec = []
        for triad in triads:
            u = set.intersection(chord_, set(triad))
            intersec.append(len(u))
        score *= max(intersec)

    # Distance penalty
    for n in chord:
        if n + 8 > note or n + 24 < note:
            score -= abs(n - note)

    # Chord should have 3 different notes
    if len(set(chord)) < 3:
        score = -np.inf

    return (score, list(chord))


def select_ancestors(scored_populaion: list, num_of_anc: int) -> list:
    """Selects best chords of population

    param scored_population: list of tuple (each elemnt is output of 
    get_fitness() method)

    param num_of_anc: number of chords to leave

    returns: list of best chords
    """

    scored_populaion.sort()
    scored_populaion = [i[1] for i in scored_populaion]
    return scored_populaion[len(scored_populaion) - num_of_anc:]


def crossover(fir: list, sec: list) -> list:
    """Implemnets crossover algorithm.
    Takes two parent chords and cross them in random point

    param fir: first parent chord
    param sec: second parent chord

    returns: child chord
    """

    idx = randint(1, 4)
    return fir[:idx] + sec[idx:]


def gen_new_population(ancestors: list) -> list:
    """Generates a list of new population from given ancestors.
    New pupulation is obtaind from pairwise crossover of
    all ancestors.

    param ancestors: list of ancestors chord

    returns: list of new population chords
    """

    new_pop = []
    for i in range(len(ancestors) - 1):
        for j in range(i + 1, len(ancestors)):
            new_pop.append(crossover(ancestors[i], ancestors[j]))

    return new_pop


def mutate(chord: list, probability: float=0.05) -> list:
    """Mutates some of the chord notes.
    Mutation occurs with given probability.
    The mutated note is shifted within random number in [-5, 5]

    returns: mutated chord
    """

    for i in range(3):
        if rand(1) < probability:
            shift = randint(-5, 6)
            chord[i] += shift
            if chord[i] < 0: chord[i] = 0
            if chord[i] > 127: chord[i] = 127
    
    return chord


def launch_genetic(note, key: list, 
                   num_iter=10 ,pop_size=45_000) -> list:
    """Performs main loop of genetic algorithm

    param note: note to which we want to generate the chord
    param key: key of the given song
    param num_iter: number of generations
    param pop_size: size of each population

    returns: best found chord
    """

    # Define number of ancestors to leave from each population
    num_of_anc = round((1 + np.sqrt(1 + 8 * pop_size)) / 2)
    # Generate initial population
    pop = randint(0, 128, (pop_size, 3))

    for _ in range(num_iter):
        # Get scored with fitness function population
        pop = [get_fitness(note, key, chord) for chord in pop]
        # Select best population chords
        pop = select_ancestors(pop, num_of_anc=num_of_anc)
        # Generate new population
        new_pop = gen_new_population(pop)
        # Concatenate new population with it's parents
        pop = pop + new_pop
        # Mutate our population
        pop = [mutate(chord) for chord in pop]

    # Score final population
    pop = [get_fitness(note, key, chord) for chord in pop]
    return select_ancestors(pop, num_of_anc=1)[0]


"""Main Function"""

def genereate_accompaniment(path: str, name_out: str):
    """
    param path: path to the .mid file to which generate accompaniment
    param name_out: name of the output .mid file
    """
    # Parse given .mid file in music21 format
    file = music21.converter.parse(path)
    # Parse given .mid file in mido format
    mid = MidiFile(path)
    mid.type = 1
    # Create new track to write the accompaniment
    track = MidiTrack()
    mid.tracks.append(track)

    # Define notes or rests to play each quarter
    notes = get_notes_on_quaters(file)
    # Fill rests with adjacent notes
    notes = fill_rests(notes)
    for note in tqdm(notes):
        # Add quarter rest if needed
        if note is None:
            track.append(Message('note_on', channel=0, note=0, velocity=0, time=mid.ticks_per_beat))
            continue
        
        # Use of genetic algorithm to generate chord for given note
        cord = launch_genetic(note, define_key(file), pop_size=1_000, num_iter=6)

        track.append(Message('note_on', channel=0, note=cord[0], velocity=50, time=0))
        track.append(Message('note_on', channel=0, note=cord[1], velocity=50, time=0))
        track.append(Message('note_on', channel=0, note=cord[2], velocity=50, time=0))

        track.append(Message('note_off', channel=0, note=cord[0], velocity=0, time=mid.ticks_per_beat))
        track.append(Message('note_off', channel=0, note=cord[1], velocity=0, time=0))
        track.append(Message('note_off', channel=0, note=cord[2], velocity=0, time=0))

    mid.save(name_out)


#genereate_accompaniment('barbiegirl_mono.mid', 'barbiegirl_gen.mid')
genereate_accompaniment('input1.mid', 'output1.mid')
genereate_accompaniment('input2.mid', 'output2.mid')
genereate_accompaniment('input3.mid', 'output3.mid')
